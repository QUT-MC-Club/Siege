package io.github.restioson.siege.game;

import io.github.restioson.siege.entity.SiegeKitStandEntity;
import io.github.restioson.siege.game.active.SiegePersonalResource;
import io.github.restioson.siege.game.active.SiegePlayer;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

public enum SiegeKit {
    ARCHER(new LiteralText("Archer Kit")),
    SOLDIER(new LiteralText("Soldier Kit")),
    CONSTRUCTOR(new LiteralText("Constructor Kit")),
    SHIELD_BEARER(new LiteralText("Shield Bearer Kit")),
    DEMOLITIONER(new LiteralText("Demolitioner Kit"));

    public static int ARROWS = 16;
    public static int PLANKS = 12;
    public static int STEAK = 10;
    public static int TNT = 1;

    public final Text name;

    SiegeKit(Text name) {
        this.name = name;
    }

    public void equipArmourStand(SiegeKitStandEntity stand) {
        switch (this) {
            case ARCHER -> {
                this.giveArcherEquipment(stand, stand.getTeam());
                stand.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.WOODEN_SWORD));
                stand.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            }
            case SOLDIER -> {
                this.giveSoldierEquipment(stand, stand.getTeam());
                stand.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                stand.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.STONE_AXE));
            }
            case CONSTRUCTOR -> {
                this.giveConstructorEquipment(stand, stand.getTeam());
                stand.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_AXE));
            }
            case SHIELD_BEARER -> {
                this.giveShieldEquipment(stand, stand.getTeam());
                stand.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
            }
            case DEMOLITIONER -> this.giveDemolitionerEquipment(stand, stand.getTeam());
        }
    }

    private void maybeGiveEnderPearl(ServerPlayerEntity player, SiegePlayer participant, SiegeConfig config) {
        if (config.defenderEnderPearl() && participant.team == SiegeTeams.DEFENDERS && player.getInventory().count(Items.ENDER_PEARL) == 0) {
            player.getInventory().insertStack(
                    ItemStackBuilder.of(Items.ENDER_PEARL)
                            .setCount(1)
                            .setName(new LiteralText("Warp to Front Lines"))
                            .addEnchantment(Enchantments.LUCK_OF_THE_SEA, 1)
                            .addLore(new LiteralText("This ender pearl will take you"))
                            .addLore(new LiteralText("to a flag in need of assistance!"))
                            .build()
            );
        }
    }

    public @Nullable String restock(ServerPlayerEntity player, SiegePlayer participant, ServerWorld world, SiegeConfig config) {
        var inventory = player.getInventory();

        switch (participant.kit) {
            case ARCHER:
                int arrowsRequired = SiegeKit.ARROWS - inventory.count(Items.ARROW);
                int arrowsToGive = participant.tryDecrementResource(SiegePersonalResource.WOOD, arrowsRequired);
                inventory.offerOrDrop(ItemStackBuilder.of(Items.ARROW).setCount(arrowsToGive).build());

                if (arrowsRequired != 0 && arrowsToGive == 0) {
                    return "You have no more arrows right now!";
                }

                break;
            case CONSTRUCTOR:
                Item planks = SiegeTeams.planksForTeam(participant.team.key());
                int planksRequired = SiegeKit.PLANKS - inventory.count(planks);
                int planksToGive = participant.tryDecrementResource(SiegePersonalResource.WOOD, planksRequired);

                if (planksRequired == SiegeKit.PLANKS) {
                    player.equipStack(EquipmentSlot.OFFHAND, ItemStackBuilder.of(planks).setCount(planksToGive).build());
                } else {
                    inventory.offerOrDrop(ItemStackBuilder.of(planks).setCount(planksToGive).build());
                }

                break;
            case DEMOLITIONER:
                int tntRequired = SiegeKit.TNT - inventory.count(Items.TNT);
                int tntToGive = participant.tryDecrementResource(SiegePersonalResource.TNT, tntRequired);
                inventory.offerOrDrop(ItemStackBuilder.of(Items.TNT).setCount(tntToGive).build());

                if (tntRequired != 0 && tntToGive == 0) {
                    return "You have no TNT right now!";
                }

                break;
            default:
                break;
        }

        int steakRequired = SiegeKit.STEAK - inventory.count(Items.COOKED_BEEF);
        inventory.offerOrDrop(ItemStackBuilder.of(Items.COOKED_BEEF).setCount(steakRequired).build());

        this.maybeGiveEnderPearl(player, participant, config);

        return null;
    }

    public void equipPlayer(ServerPlayerEntity player, SiegePlayer participant, ServerWorld world, SiegeConfig config) {
        var inventory = player.getInventory();
        int wood = inventory.count(Items.ARROW) + inventory.count(SiegeTeams.planksForTeam(SiegeTeams.ATTACKERS.key()))
                + inventory.count(SiegeTeams.planksForTeam(SiegeTeams.DEFENDERS.key()));
        int tnt = inventory.count(Items.TNT);
        participant.incrementResource(SiegePersonalResource.WOOD, wood);
        participant.incrementResource(SiegePersonalResource.TNT, tnt);

        inventory.clear();
        player.clearStatusEffects();
        GameTeam team = participant.team;

        switch (this) {
            case ARCHER -> {
                this.giveArcherKit(player, participant);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, Integer.MAX_VALUE, 0, false, false, true));
            }
            case SOLDIER -> this.giveSoldierKit(player, team);
            case CONSTRUCTOR -> this.giveConstructorKit(player, participant);
            case SHIELD_BEARER -> {
                this.giveShieldKit(player, team);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, Integer.MAX_VALUE, 0, false, false, true));
            }
            case DEMOLITIONER -> {
                this.giveDemolitionerKit(player, team);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, Integer.MAX_VALUE, 0, false, false, true));
            }
        }

        inventory.insertStack(ItemStackBuilder.of(Items.COOKED_BEEF).setCount(STEAK).build());

        this.maybeGiveEnderPearl(player, participant, config);
        this.restock(player, participant, world, config);
    }

    // give{x}Equipment is for armour stands and players

    private void giveArcherEquipment(LivingEntity entity, GameTeam team) {
        entity.equipStack(EquipmentSlot.HEAD, this.dyedArmor(team, Items.LEATHER_HELMET));
        entity.equipStack(EquipmentSlot.CHEST, this.dyedArmor(team, Items.LEATHER_CHESTPLATE));
        entity.equipStack(EquipmentSlot.LEGS, this.dyedArmor(team, Items.LEATHER_LEGGINGS));
        entity.equipStack(EquipmentSlot.FEET, this.dyedArmor(team, Items.LEATHER_BOOTS));
    }

    private void giveSoldierEquipment(LivingEntity entity, GameTeam team) {
        entity.equipStack(EquipmentSlot.HEAD, this.dyedArmor(team, Items.LEATHER_HELMET));
        entity.equipStack(EquipmentSlot.CHEST, ItemStackBuilder.of(Items.DIAMOND_CHESTPLATE).setUnbreakable().build());
        entity.equipStack(EquipmentSlot.LEGS, this.dyedArmor(team, Items.LEATHER_LEGGINGS));
        entity.equipStack(EquipmentSlot.FEET, ItemStackBuilder.of(Items.IRON_BOOTS).setUnbreakable().build());
    }

    private void giveConstructorEquipment(LivingEntity entity, GameTeam team) {
        entity.equipStack(EquipmentSlot.HEAD, this.dyedArmor(team, Items.LEATHER_HELMET));
        entity.equipStack(EquipmentSlot.CHEST, ItemStackBuilder.of(Items.IRON_CHESTPLATE).setUnbreakable().build());
        entity.equipStack(EquipmentSlot.LEGS, this.dyedArmor(team, Items.LEATHER_LEGGINGS));
        entity.equipStack(EquipmentSlot.FEET, this.dyedArmor(team, Items.LEATHER_BOOTS));
        entity.equipStack(EquipmentSlot.OFFHAND, new ItemStack(SiegeTeams.planksForTeam(team.key())));
    }

    private void giveShieldEquipment(LivingEntity entity, GameTeam team) {
        entity.equipStack(EquipmentSlot.HEAD, this.dyedArmor(team, Items.LEATHER_HELMET));
        entity.equipStack(EquipmentSlot.CHEST, ItemStackBuilder.of(Items.IRON_CHESTPLATE).setUnbreakable().build());
        entity.equipStack(EquipmentSlot.LEGS, ItemStackBuilder.of(Items.IRON_LEGGINGS).setUnbreakable().build());
        entity.equipStack(EquipmentSlot.FEET, this.dyedArmor(team, Items.LEATHER_BOOTS));
        entity.equipStack(EquipmentSlot.OFFHAND, ItemStackBuilder.of(Items.SHIELD).setUnbreakable().build());
    }

    private void giveDemolitionerEquipment(LivingEntity entity, GameTeam team) {
        entity.equipStack(EquipmentSlot.HEAD, this.dyedArmor(team, Items.LEATHER_HELMET));
        entity.equipStack(EquipmentSlot.CHEST, this.dyedArmor(team, Items.LEATHER_CHESTPLATE));
        entity.equipStack(EquipmentSlot.LEGS, this.dyedArmor(team, Items.LEATHER_LEGGINGS));
        entity.equipStack(EquipmentSlot.FEET, this.dyedArmor(team, Items.LEATHER_BOOTS));
        entity.equipStack(EquipmentSlot.MAINHAND, ItemStackBuilder.of(Items.TNT).setUnbreakable().build());
    }

    private ItemStack dyedArmor(GameTeam team, Item leatherHelmet) {
        return ItemStackBuilder.of(leatherHelmet).setDyeColor(team.config().dyeColor().getRgb()).setUnbreakable().build();
    }

    // give{x}Kit is for players only

    private void giveArcherKit(PlayerEntity player, SiegePlayer participant) {
        this.giveArcherEquipment(player, participant.team);
        player.getInventory().insertStack(ItemStackBuilder.of(Items.WOODEN_SWORD).setUnbreakable().build());
        player.getInventory().insertStack(
                ItemStackBuilder.of(Items.BOW)
                        .addEnchantment(Enchantments.PUNCH, 1)
                        .setUnbreakable()
                        .build()
        );
    }

    private void giveSoldierKit(PlayerEntity player, GameTeam team) {
        this.giveSoldierEquipment(player, team);
        player.getInventory().insertStack(ItemStackBuilder.of(Items.IRON_SWORD).setUnbreakable().build());
        player.getInventory().insertStack(ItemStackBuilder.of(Items.STONE_AXE).setUnbreakable().build());
    }

    private void giveConstructorKit(PlayerEntity player, SiegePlayer participant) {
        this.giveConstructorEquipment(player, participant.team);
        player.getInventory().insertStack(ItemStackBuilder.of(Items.WOODEN_SWORD).setUnbreakable().build());
        player.getInventory().insertStack(ItemStackBuilder.of(Items.WOODEN_AXE).setUnbreakable().build());
        player.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.AIR));
    }

    private void giveShieldKit(PlayerEntity player, GameTeam team) {
        this.giveShieldEquipment(player, team);
        player.getInventory().insertStack(ItemStackBuilder.of(Items.STONE_SWORD).setUnbreakable().build());
    }

    private void giveDemolitionerKit(PlayerEntity player, GameTeam team) {
        this.giveDemolitionerEquipment(player, team);
        player.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.AIR));
    }
}
