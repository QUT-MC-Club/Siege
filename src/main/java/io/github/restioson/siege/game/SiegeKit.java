package io.github.restioson.siege.game;

import io.github.restioson.siege.entity.SiegeKitStandEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

public enum SiegeKit {
    ARCHER(new LiteralText("Archer Kit")),
    SOLDIER(new LiteralText("Soldier Kit")),
    CONSTRUCTOR(new LiteralText("Constructor Kit")),
    SHIELD_BEARER(new LiteralText("Shield Bearer Kit"));

    public final Text name;

    SiegeKit(Text name) {
        this.name = name;
    }

    private static Item planksForTeam(GameTeam team) {
        if (team == SiegeTeams.ATTACKERS) {
            return Items.ACACIA_PLANKS;
        } else {
            return Items.BIRCH_PLANKS;
        }
    }

    public void equipArmourStand(SiegeKitStandEntity stand) {
        switch (this) {
            case ARCHER:
                this.giveArcherEquipment(stand, stand.team);
                stand.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.WOODEN_SWORD));
                stand.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                break;
            case SOLDIER:
                this.giveSoldierEquipment(stand, stand.team);
                stand.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                stand.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.IRON_AXE));
                break;
            case CONSTRUCTOR:
                this.giveConstructorEquipment(stand, stand.team);
                stand.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_AXE));
                break;
            case SHIELD_BEARER:
                this.giveShieldEquipment(stand, stand.team);
                stand.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                break;
        }
    }

    public void equipPlayer(ServerPlayerEntity player, GameTeam team) {
        player.inventory.clear();

        switch (this) {
            case ARCHER:
                this.giveArcherKit(player, team);
                break;
            case SOLDIER:
                this.giveSoldierKit(player, team);
                break;
            case CONSTRUCTOR:
                this.giveConstructorKit(player, team);
                break;
            case SHIELD_BEARER:
                this.giveShieldKit(player, team);
                break;
        }

        player.inventory.insertStack(ItemStackBuilder.of(Items.COOKED_BEEF).setCount(10).build());
    }

    // give{x}Equipment is for armour stands and players

    private void giveArcherEquipment(LivingEntity entity, GameTeam team) {
        entity.equipStack(EquipmentSlot.HEAD, ItemStackBuilder.of(Items.LEATHER_HELMET).setColor(team.getColor()).setUnbreakable().build());
        entity.equipStack(EquipmentSlot.CHEST, ItemStackBuilder.of(Items.LEATHER_CHESTPLATE).setColor(team.getColor()).setUnbreakable().build());
        entity.equipStack(EquipmentSlot.LEGS, ItemStackBuilder.of(Items.LEATHER_LEGGINGS).setColor(team.getColor()).setUnbreakable().build());
        entity.equipStack(EquipmentSlot.FEET, ItemStackBuilder.of(Items.LEATHER_BOOTS).setColor(team.getColor()).setUnbreakable().build());
    }

    private void giveSoldierEquipment(LivingEntity entity, GameTeam team) {
        entity.equipStack(EquipmentSlot.HEAD, ItemStackBuilder.of(Items.LEATHER_HELMET).setColor(team.getColor()).setUnbreakable().build());
        entity.equipStack(EquipmentSlot.CHEST, ItemStackBuilder.of(Items.IRON_CHESTPLATE).setUnbreakable().build());
        entity.equipStack(EquipmentSlot.LEGS, ItemStackBuilder.of(Items.LEATHER_LEGGINGS).setColor(team.getColor()).setUnbreakable().build());
        entity.equipStack(EquipmentSlot.FEET, ItemStackBuilder.of(Items.IRON_BOOTS).setUnbreakable().build());
    }

    private void giveConstructorEquipment(LivingEntity entity, GameTeam team) {
        entity.equipStack(EquipmentSlot.HEAD, ItemStackBuilder.of(Items.LEATHER_HELMET).setColor(team.getColor()).setUnbreakable().build());
        entity.equipStack(EquipmentSlot.CHEST, ItemStackBuilder.of(Items.LEATHER_CHESTPLATE).setColor(team.getColor()).setUnbreakable().build());
        entity.equipStack(EquipmentSlot.LEGS, ItemStackBuilder.of(Items.LEATHER_LEGGINGS).setColor(team.getColor()).setUnbreakable().build());
        entity.equipStack(EquipmentSlot.FEET, ItemStackBuilder.of(Items.LEATHER_BOOTS).setColor(team.getColor()).setUnbreakable().build());
        entity.equipStack(EquipmentSlot.OFFHAND, new ItemStack(SiegeKit.planksForTeam(team)));
    }

    private void giveShieldEquipment(LivingEntity entity, GameTeam team) {
        entity.equipStack(EquipmentSlot.HEAD, ItemStackBuilder.of(Items.LEATHER_HELMET).setColor(team.getColor()).build());
        entity.equipStack(EquipmentSlot.CHEST, ItemStackBuilder.of(Items.IRON_CHESTPLATE).setUnbreakable().build());
        entity.equipStack(EquipmentSlot.LEGS, ItemStackBuilder.of(Items.LEATHER_LEGGINGS).setColor(team.getColor()).setUnbreakable().build());
        entity.equipStack(EquipmentSlot.FEET, ItemStackBuilder.of(Items.LEATHER_BOOTS).setColor(team.getColor()).setUnbreakable().build());
        entity.equipStack(EquipmentSlot.OFFHAND, ItemStackBuilder.of(Items.SHIELD).setUnbreakable().build());
    }

    // give{x}Kit is for players only

    private void giveArcherKit(PlayerEntity player, GameTeam team) {
        this.giveArcherEquipment(player, team);
        player.inventory.insertStack(ItemStackBuilder.of(Items.WOODEN_SWORD).setUnbreakable().build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.BOW).setUnbreakable().build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.ARROW).setCount(16).build());
    }

    private void giveSoldierKit(PlayerEntity player, GameTeam team) {
        this.giveSoldierEquipment(player, team);
        player.inventory.insertStack(ItemStackBuilder.of(Items.IRON_SWORD).setUnbreakable().build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.IRON_AXE).setUnbreakable().build());
    }

    private void giveConstructorKit(PlayerEntity player, GameTeam team) {
        this.giveConstructorEquipment(player, team);
        player.equipStack(EquipmentSlot.OFFHAND, ItemStackBuilder.of(SiegeKit.planksForTeam(team)).setCount(16).build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.WOODEN_SWORD).setUnbreakable().build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.WOODEN_AXE).setUnbreakable().build());
        player.inventory.insertStack(ItemStackBuilder.of(Items.LADDER).setCount(4).build());
    }

    private void giveShieldKit(PlayerEntity player, GameTeam team) {
        this.giveShieldEquipment(player, team);
        player.inventory.insertStack(ItemStackBuilder.of(Items.IRON_SWORD).setUnbreakable().build());
    }
}
