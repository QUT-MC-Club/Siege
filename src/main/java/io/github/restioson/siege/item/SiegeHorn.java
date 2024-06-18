package io.github.restioson.siege.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polymer.core.api.item.PolymerItem;
import io.github.restioson.siege.Siege;
import io.github.restioson.siege.game.active.SiegeActive;
import io.github.restioson.siege.game.active.SiegePlayer;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.InstrumentTags;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class SiegeHorn extends GoatHornItem implements PolymerItem {
    private static final int COOLDOWN_TICKS = 30 * 20;
    private static final int EFFECT_RADIUS = 10;
    private static final String HORN_DATA_KEY = "siege_horn_data";
    private static final Codec<StatusEffectInstance> HORN_DATA_CODEC = RecordCodecBuilder.create(instance -> instance.group(Codec.INT.fieldOf("duration").forGetter(StatusEffectInstance::getDuration), Registries.STATUS_EFFECT.getCodec().fieldOf("effect").forGetter(StatusEffectInstance::getEffectType), Codec.INT.fieldOf("amplifier").forGetter(StatusEffectInstance::getAmplifier)).apply(instance, (duration, effect, amplifier) -> new StatusEffectInstance(effect, duration, amplifier)));

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

    public static void onUse(SiegeActive active, ServerPlayerEntity userPlayer, SiegePlayer user, ItemStack stack) {
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

        var cooldownMgr = userPlayer.getItemCooldownManager();
        cooldownMgr.set(SiegeItems.HORN, COOLDOWN_TICKS);
        cooldownMgr.set(((SiegeHorn) stack.getItem()).getPolymerItem(stack, userPlayer), COOLDOWN_TICKS);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.GOAT_HORN;
    }
}
