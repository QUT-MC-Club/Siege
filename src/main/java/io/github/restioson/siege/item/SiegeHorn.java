package io.github.restioson.siege.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polymer.core.api.item.PolymerItem;
import io.github.restioson.siege.Siege;
import io.github.restioson.siege.game.active.SiegeActive;
import io.github.restioson.siege.game.active.SiegePlayer;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.InstrumentTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

import static eu.pb4.polymer.core.api.item.PolymerItemUtils.NON_ITALIC_STYLE;

public class SiegeHorn extends GoatHornItem implements PolymerItem {
    private static final int COOLDOWN_TICKS = 30 * 20;
    private static final int SOUND_RADIUS = 64;
    private static final int EFFECT_RADIUS = 15;
    private static final String HORN_DATA_KEY = "siege_horn_data";
    private static final Codec<StatusEffectInstance> HORN_DATA_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("duration").forGetter(StatusEffectInstance::getDuration),
            Registries.STATUS_EFFECT.getCodec().fieldOf("effect").forGetter(StatusEffectInstance::getEffectType),
            Codec.INT.fieldOf("amplifier").forGetter(StatusEffectInstance::getAmplifier)
    ).apply(instance, (duration, effect, amplifier) -> new StatusEffectInstance(effect, duration, amplifier)));

    public SiegeHorn(Settings settings) {
        super(settings, InstrumentTags.GOAT_HORNS);
    }

    public static ItemStack getStack(RegistryKey<Instrument> instrument, Stream<StatusEffectInstance> effects) {
        ItemStack stack = SiegeHorn.getStackForInstrument(SiegeItems.HORN, Registries.INSTRUMENT.entryOf(instrument));

        NbtList hornData = new NbtList();
        effects.map(effect -> HORN_DATA_CODEC.encodeStart(NbtOps.INSTANCE, effect)).map(res -> res.getOrThrow(false, err -> Siege.LOGGER.error("Failed to write horn data: {}", err))).forEach(hornData::add);
        stack.getOrCreateNbt().put(HORN_DATA_KEY, hornData);

        return stack;
    }

    public static TypedActionResult<ItemStack> onUse(SiegeActive active, ServerPlayerEntity userPlayer, SiegePlayer user, ItemStack stack, Hand hand) {
        var result = stack.use(active.world, userPlayer, hand);

        if (!result.getResult().isAccepted()) {
            return result; // Fail early
        }

        for (var nbt : stack.getOrCreateNbt().getList(HORN_DATA_KEY, NbtElement.COMPOUND_TYPE)) {
            var effect = HORN_DATA_CODEC.decode(NbtOps.INSTANCE, nbt).getOrThrow(false, err -> Siege.LOGGER.error("Failed to load horn data: {}", err)).getFirst();

            for (var entry : active.participants.entrySet()) {
                var participant = entry.getValue();
                var player = entry.getKey().getEntity(active.world);

                if (participant.team != user.team || player == null || !player.getBlockPos().isWithinDistance(userPlayer.getPos(), EFFECT_RADIUS)) {
                    continue;
                }

                player.addStatusEffect(new StatusEffectInstance(effect)); // Copy effect
            }
        }

        AreaEffectCloudEntity aoeCloud = new AreaEffectCloudEntity(
                active.world,
                userPlayer.getX(),
                userPlayer.getY(),
                userPlayer.getZ()
        );
        aoeCloud.setColor(user.team.config().fireworkColor().getRgb());
        aoeCloud.setRadius(EFFECT_RADIUS);
        aoeCloud.setDuration(1);
        active.world.spawnEntity(aoeCloud);

        var cooldownMgr = userPlayer.getItemCooldownManager();
        cooldownMgr.set(SiegeItems.HORN, COOLDOWN_TICKS);
        cooldownMgr.set(((SiegeHorn) stack.getItem()).getPolymerItem(stack, userPlayer), COOLDOWN_TICKS);

        return result;
    }

    // TODO HACK: copied from vanilla to change distance. Really we should use a custom instrument
    private static void playSound(World world, PlayerEntity player, Instrument instrument) {
        SoundEvent soundEvent = instrument.soundEvent().value();
        float f = SOUND_RADIUS / 16.0F;
        world.playSoundFromEntity(player, player, soundEvent, SoundCategory.RECORDS, f, 1.0F);
        world.emitGameEvent(GameEvent.INSTRUMENT_PLAY, player.getPos(), GameEvent.Emitter.of(player));
    }

    // TODO HACK: copied from vanilla to override playSound
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        Optional<? extends RegistryEntry<Instrument>> optional = this.getInstrument(itemStack);
        if (optional.isPresent()) {
            Instrument instrument = (Instrument) ((RegistryEntry<?>) optional.get()).value();
            user.setCurrentHand(hand);
            playSound(world, user, instrument);
            user.incrementStat(Stats.USED.getOrCreateStat(this));
            return TypedActionResult.consume(itemStack);
        } else {
            return TypedActionResult.fail(itemStack);
        }
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipContext context, @Nullable ServerPlayerEntity player) {
        var name = Text.translatable("item.siege.captains_horn").setStyle(NON_ITALIC_STYLE);

        var stack = GoatHornItem
                .getStackForInstrument(
                        Items.GOAT_HORN,
                        // This cast is fine since SiegeHorn is a subclass of GoatHornItem
                        ((GoatHornItem) itemStack.getItem()).getInstrument(itemStack).orElseThrow()
                )
                .setCustomName(name);
        stack.addEnchantment(null, 1);
        return stack;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.GOAT_HORN;
    }
}
