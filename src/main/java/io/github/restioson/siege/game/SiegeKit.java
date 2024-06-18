package io.github.restioson.siege.game;

import io.github.restioson.siege.entity.SiegeKitStandEntity;
import io.github.restioson.siege.game.active.SiegePersonalResource;
import io.github.restioson.siege.game.active.SiegePlayer;
import io.github.restioson.siege.item.SiegeHorn;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

import java.util.List;
import java.util.stream.Stream;

public record SiegeKit(Text name, List<KitEquipable> equipment, List<AbstractKitResource> resources,
                       List<StatusEffectInstance> statusEffects) {
    public static final SiegeKit SOLDIER = new SiegeKit(
            Text.translatable("game.siege.kit.kits.soldier"),
            List.of(
                    new KitEquipment(Items.LEATHER_HELMET),
                    new KitEquipment(Items.DIAMOND_CHESTPLATE),
                    new KitEquipment(Items.LEATHER_LEGGINGS),
                    new KitEquipment(Items.IRON_BOOTS),
                    new KitEquipment(Items.IRON_SWORD, EquipmentSlot.MAINHAND),
                    new KitEquipment(Items.STONE_AXE, EquipmentSlot.OFFHAND)
            ),
            List.of(),
            List.of()
    );
    public static final SiegeKit SHIELD_BEARER = new SiegeKit(
            Text.translatable("game.siege.kit.kits.shield_bearer"),
            List.of(
                    new KitEquipment(Items.LEATHER_HELMET),
                    new KitEquipment(Items.IRON_CHESTPLATE),
                    new KitEquipment(Items.IRON_LEGGINGS),
                    new KitEquipment(Items.LEATHER_BOOTS),
                    new KitEquipment(Items.STONE_SWORD, EquipmentSlot.MAINHAND),
                    new KitEquipment(Items.SHIELD, EquipmentSlot.OFFHAND)
            ),
            List.of(),
            List.of(kitEffect(StatusEffects.RESISTANCE))
    );
    public static final SiegeKit ARCHER = new SiegeKit(
            Text.translatable("game.siege.kit.kits.archer"),
            List.of(
                    new KitEquipment(Items.LEATHER_HELMET),
                    new KitEquipment(Items.LEATHER_CHESTPLATE),
                    new KitEquipment(Items.LEATHER_LEGGINGS),
                    new KitEquipment(Items.LEATHER_BOOTS),
                    new KitEquipment(Items.WOODEN_SWORD, EquipmentSlot.OFFHAND),
                    new KitEquipment(
                            Items.BOW,
                            Items.BOW,
                            List.of(new EnchantmentLevelEntry(Enchantments.PUNCH, 1)),
                            EquipmentSlot.MAINHAND,
                            null
                    )
            ),
            List.of(
                    new KitResource(
                            Text.translatable("game.siege.kit.items.arrows"),
                            Items.ARROW,
                            SiegePersonalResource.WOOD,
                            16
                    )
            ),
            List.of(kitEffect(StatusEffects.SPEED))
    );
    public static final SiegeKit CONSTRUCTOR = new SiegeKit(
            Text.translatable("game.siege.kit.kits.constructor"),
            List.of(
                    new KitEquipment(Items.LEATHER_HELMET),
                    new KitEquipment(Items.IRON_CHESTPLATE),
                    new KitEquipment(Items.LEATHER_LEGGINGS),
                    new KitEquipment(Items.LEATHER_BOOTS),
                    new KitEquipment(Items.WOODEN_AXE, EquipmentSlot.MAINHAND),
                    new KitEquipment(Items.WOODEN_SWORD)
            ),
            List.of(KitResource.PLANKS),
            List.of()
    );
    public static final SiegeKit DEMOLITIONER = new SiegeKit(
            Text.translatable("game.siege.kit.kits.demolitioner"),
            List.of(
                    new KitEquipment(Items.LEATHER_HELMET),
                    new KitEquipment(Items.LEATHER_CHESTPLATE),
                    new KitEquipment(Items.LEATHER_LEGGINGS),
                    new KitEquipment(Items.LEATHER_BOOTS),
                    new KitEquipment(Items.WOODEN_SWORD, EquipmentSlot.MAINHAND)
            ),
            List.of(
                    new KitResource(
                            Text.translatable("game.siege.kit.items.tnt"),
                            Items.TNT,
                            SiegePersonalResource.TNT,
                            EquipmentSlot.OFFHAND,
                            2
                    )
            ),
            List.of(kitEffect(StatusEffects.GLOWING))
    );

    public static final SiegeKit CAPTAIN = new SiegeKit(
            Text.translatable("game.siege.kit.kits.captain"),
            List.of(
                    new KitEquipment(
                            Items.RED_BANNER,
                            Items.BLUE_BANNER,
                            EquipmentSlot.HEAD,
                            EquipmentSlot.HEAD
                    ),
                    new KitEquipment(Items.GOLDEN_CHESTPLATE),
                    new KitEquipment(Items.LEATHER_LEGGINGS),
                    new KitEquipment(Items.GOLDEN_CHESTPLATE),
                    new KitEquipment(Items.WOODEN_SWORD, EquipmentSlot.MAINHAND),
                    new KitEquipable() {
                        @Override
                        public EquipmentSlot getArmorStandSlot() {
                            return EquipmentSlot.OFFHAND;
                        }

                        @Override
                        public EquipmentSlot getPlayerSlot() {
                            return EquipmentSlot.OFFHAND;
                        }

                        @Override
                        public ItemStack buildItemStack(GameTeam team) {
                            return SiegeHorn.getStack(
                                    team == SiegeTeams.DEFENDERS ? Instruments.PONDER_GOAT_HORN : Instruments.CALL_GOAT_HORN,
                                    Stream.of(
                                            new StatusEffectInstance(
                                                    StatusEffects.STRENGTH,
                                                    10 * 20
                                            ),
                                            new StatusEffectInstance(
                                                    StatusEffects.SPEED,
                                                    10 * 20
                                            )
                                    )
                            );
                        }
                    }
            ),
            List.of(),
            List.of()
    );

    public SiegeKit(Text name, List<KitEquipable> equipment, List<AbstractKitResource> resources, List<StatusEffectInstance> statusEffects) {
        this.name = name;
        this.equipment = equipment;
        this.resources = Stream.concat(resources.stream(), Stream.of(KitResource.STEAK, KitResource.FIREWORK)).toList();
        this.statusEffects = statusEffects;
    }

    private static StatusEffectInstance kitEffect(StatusEffect effect) {
        return new StatusEffectInstance(effect, 1);
    }

    private static Text restockMessage(List<RestockResult> restockResults, long time, boolean isEquip) {
        var text = Text.empty();

        if (restockResults.stream().allMatch(RestockResult::success)) {
            if (isEquip) {
                return Text.empty();
            }

            text.append(Text.literal("Successfully restocked ").formatted(Formatting.DARK_GREEN));
        } else if (restockResults.stream().noneMatch(RestockResult::success)) {
            text.append(Text.literal("Failed to restock ").formatted(Formatting.RED));
        } else {
            text.append(Text.literal("Partially restocked ").formatted(Formatting.YELLOW));
        }

        boolean first = true;
        var iter = restockResults.iterator();
        while (iter.hasNext()) {
            var result = iter.next();
            if (!first) {
                if (!iter.hasNext()) {
                    text.append(Text.literal(" and "));
                } else {
                    text.append(Text.literal(", "));
                }
            }
            first = false;

            var colour = result.success ? Formatting.DARK_GREEN : Formatting.RED;

            var restockingIn = result.current != 0 || result.resource == null ? "" : String.format(" (more in %ss)", result.resource.getNextRefreshSecs(time));

            text.append(result.name.copy().formatted(colour));

            if (!result.success) {
                text.append(Text.literal(restockingIn).formatted(colour));
            } else if (result.max != 0) {
                text.append(Text.literal(String.format(" (%d/%d left)", result.current, result.max)).formatted(colour));
            }
        }

        return text;
    }

    public void equipArmourStand(SiegeKitStandEntity stand) {
        var team = stand.getTeam();

        for (var item : this.equipment) {
            ItemStack stack = item.buildItemStack(team);
            EquipmentSlot slot;
            if (item.getArmorStandSlot() != null) {
                slot = item.getArmorStandSlot();
            } else if (stack.getItem() instanceof Equipment equipmentItem) {
                slot = equipmentItem.getSlotType();
            } else {
                continue;
            }

            stand.equipStack(slot, item.buildItemStack(team));
        }

        for (var item : this.resources) {
            if (item.equipmentSlot() != null) {
                stand.equipStack(item.equipmentSlot(), item.itemStackBuilder(team).build());
            }
        }
    }

    public void returnResources(ServerPlayerEntity player, SiegePlayer participant) {
        var inventory = player.getInventory();

        for (var invList : List.of(inventory.main, inventory.offHand)) {
            for (var stack : invList) {
                for (var resource : this.resources) {
                    if (resource.resource() == null) {
                        continue;
                    }

                    if (resource.itemForTeam(participant.team) == stack.getItem()) {
                        participant.incrementResource(resource.resource(), stack.getCount());
                        break;
                    }
                }
            }
        }

        inventory.clear();
    }

    public void equipPlayer(ServerPlayerEntity player, SiegePlayer participant, SiegeConfig config, long time) {
        var inventory = player.getInventory();
        var team = participant.team;

        for (var item : this.equipment) {
            var stack = item.buildItemStack(team);
            if (item.getPlayerSlot() != null) {
                player.equipStack(item.getPlayerSlot(), stack);
            } else if (stack.getItem() instanceof Equipment equipmentItem) {
                player.equipStack(equipmentItem.getSlotType(), stack);
            } else {
                inventory.offerOrDrop(stack);
            }
        }

        var result = this.restock(player, participant, config, time, true);
        if (!result.getSiblings().isEmpty()) {
            player.sendMessage(result.copy().formatted(Formatting.BOLD), true);
        }

        player.clearStatusEffects();
        for (var statusEffect : this.statusEffects) {
            player.addStatusEffect(new StatusEffectInstance(statusEffect)); // Copy
        }
    }

    // TODO teleporting GUI does not work
    private void maybeGiveEnderPearl(ServerPlayerEntity player, SiegePlayer participant, SiegeConfig config) {
        if (config.defenderEnderPearl() && participant.team == SiegeTeams.DEFENDERS && player.getInventory().count(Items.ENDER_PEARL) == 0) {
            player.getInventory().insertStack(
                    ItemStackBuilder.of(Items.ENDER_PEARL)
                            .setCount(1)
                            .setName(Text.literal("Warp to Front Lines"))
                            .addEnchantment(Enchantments.LUCK_OF_THE_SEA, 1)
                            .addLore(Text.literal("This ender pearl will take you"))
                            .addLore(Text.literal("to a flag in need of assistance!"))
                            .build()
            );
        }
    }

    public Text restock(ServerPlayerEntity player, SiegePlayer participant, SiegeConfig config, long time) {
        return this.restock(player, participant, config, time, false);
    }

    private Text restock(ServerPlayerEntity player, SiegePlayer participant, SiegeConfig config, long time, boolean isEquip) {
        this.maybeGiveEnderPearl(player, participant, config);

        var results = this.resources
                .stream()
                .map(resource -> resource.restock(player, participant))
                .toList();

        return restockMessage(results, time, isEquip);
    }

    public interface KitEquipable {
        @Nullable
        EquipmentSlot getArmorStandSlot();

        @Nullable
        EquipmentSlot getPlayerSlot();

        ItemStack buildItemStack(GameTeam team);
    }

    public interface AbstractKitResource {
        default RestockResult restock(ServerPlayerEntity player, SiegePlayer participant) {
            var inventory = player.getInventory();
            var team = participant.team;
            var item = this.itemForTeam(team);
            var resource = this.resource();

            int required = this.max() - inventory.count(item);
            int toGive = resource != null ? participant.tryDecrementResource(resource, required) : required;

            var stack = this.itemStackBuilder(team).setCount(toGive).build();

            if (this.equipmentSlot() != null && required == this.max()) {
                player.equipStack(this.equipmentSlot(), stack);
            } else {
                inventory.offerOrDrop(stack);
            }

            if (resource != null) {
                return new RestockResult(participant.getResourceAmount(resource), resource.max, required == 0 || toGive > 0, resource, this.name());
            } else {
                return new RestockResult(0, 0, true, null, this.name());
            }
        }

        Text name();

        int max();

        Item itemForTeam(GameTeam team);

        ItemStackBuilder itemStackBuilder(GameTeam team);

        @Nullable
        SiegePersonalResource resource();

        default @Nullable EquipmentSlot equipmentSlot() {
            return null;
        }
    }

    private record KitEquipment(Item attackerItem, Item defenderItem, List<EnchantmentLevelEntry> enchantments,
                                @Nullable EquipmentSlot armourStandSlot,
                                @Nullable EquipmentSlot playerSlot) implements KitEquipable {
        public KitEquipment(Item item) {
            this(item, item, List.of(), null, null);
        }

        public KitEquipment(Item item, EquipmentSlot armourStandSlot) {
            this(item, item, List.of(), armourStandSlot, null);
        }

        public KitEquipment(Item attackerItem, Item defenderItem, EquipmentSlot armourStandSlot, EquipmentSlot playerSlot) {
            this(attackerItem, defenderItem, List.of(), armourStandSlot, playerSlot);
        }

        public Item itemForTeam(GameTeam team) {
            return team == SiegeTeams.DEFENDERS ? this.defenderItem : this.attackerItem;
        }

        @Override
        public EquipmentSlot getArmorStandSlot() {
            return this.armourStandSlot;
        }

        @Override
        public EquipmentSlot getPlayerSlot() {
            return this.playerSlot;
        }

        public ItemStack buildItemStack(GameTeam team) {
            var builder = ItemStackBuilder
                    .of(this.itemForTeam(team))
                    .setCount(1)
                    .setUnbreakable()
                    .setDyeColor(team.config().dyeColor().getRgb());

            for (var enchantment : this.enchantments) {
                builder.addEnchantment(enchantment.enchantment, enchantment.level);
            }

            return builder.build();
        }
    }

    private static final class Firework implements AbstractKitResource {
        @Override
        public Text name() {
            return Text.translatable("game.siege.kit.items.flare");
        }

        @Override
        public int max() {
            return 5;
        }

        @Override
        public Item itemForTeam(GameTeam team) {
            return Items.FIREWORK_ROCKET; // All fireworks are the same Item
        }

        @Override
        public ItemStackBuilder itemStackBuilder(GameTeam team) {
            return ItemStackBuilder.firework(
                    team.config().fireworkColor().getRgb(),
                    2,
                    FireworkRocketItem.Type.SMALL_BALL
            );
        }

        @Override
        public SiegePersonalResource resource() {
            return SiegePersonalResource.FLARES;
        }
    }

    /**
     * A restockable item in a kit (e.g. arrows)
     */
    public record KitResource(@Override Text name, Item attackerItem, Item defenderItem,
                              @Override @Nullable SiegePersonalResource resource,
                              @Override @Nullable EquipmentSlot equipmentSlot,
                              @Override int max) implements AbstractKitResource {
        public static final KitResource STEAK = new KitResource(Items.COOKED_BEEF.getName(), Items.COOKED_BEEF, null, null, 10);
        public static final AbstractKitResource FIREWORK = new Firework();
        public static final KitResource PLANKS = new KitResource(
                Text.translatable("game.siege.kit.items.wood"),
                Items.CHERRY_PLANKS,
                Items.BIRCH_PLANKS,
                SiegePersonalResource.WOOD,
                EquipmentSlot.OFFHAND,
                12
        );

        public KitResource(Text name, Item item, @Nullable SiegePersonalResource resource, EquipmentSlot equipmentSlot, int max) {
            this(name, item, item, resource, equipmentSlot, max);
        }

        public KitResource(Text name, Item item, @Nullable SiegePersonalResource resource, int max) {
            this(name, item, item, resource, null, max);
        }

        public ItemStackBuilder itemStackBuilder(GameTeam team) {
            return ItemStackBuilder.of(this.itemForTeam(team));
        }

        @Override
        public Item itemForTeam(GameTeam team) {
            return team == SiegeTeams.DEFENDERS ? this.defenderItem : this.attackerItem;
        }
    }

    /**
     * The result of trying to restock a kit.
     *
     * @param current  The current amount of {@link SiegePersonalResource} that the player has
     * @param max      The max amount of {@link SiegePersonalResource} that the player can have
     * @param success  Whether the restocking was successful
     * @param resource The {@link SiegePersonalResource} that the player tried to restock
     * @param name     The name of the item that they tried to restock. This isn't the same as the
     *                 {@link SiegePersonalResource}'s name, because arrows and wood both restock from the same pool, for
     *                 example.
     */
    public record RestockResult(int current, int max, boolean success, SiegePersonalResource resource, Text name) {
    }
}
