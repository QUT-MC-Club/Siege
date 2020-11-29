package io.github.restioson.siege.entity;

import io.github.restioson.siege.game.SiegeTeams;
import io.github.restioson.siege.game.map.SiegeKitStandLocation;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

public final class SiegeKitStandEntity extends ArmorStandEntity {
    private final KitType type;
    private final GameTeam team;

    public SiegeKitStandEntity(World world, SiegeKitStandLocation stand) {
        super(EntityType.ARMOR_STAND, world);
        this.type = stand.type;
        this.team = stand.team;

        this.setPos(stand.pos.x, stand.pos.y, stand.pos.z);

        this.setCustomName(this.type.name);
        this.setInvulnerable(true);
        this.setCustomNameVisible(true);

        switch (this.type) {
            case ARCHER:
                this.giveArcherEquipment(this);
                this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.WOODEN_SWORD));
                break;
            case SOLDIER:
                this.giveSoldierEquipment(this);
                this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.IRON_AXE));
                break;
            case CONSTRUCTOR:
                this.giveConstructorEquipment(this);
                break;
            case SHIELD_BEARER:
                this.giveShieldEquipment(this);
                break;
        }

    }

    public Item planksForTeam() {
        if (this.team == SiegeTeams.ATTACKERS) {
            return Items.ACACIA_PLANKS;
        } else {
            return Items.BIRCH_PLANKS;
        }
    }

    @Override
    public ActionResult interactAt(PlayerEntity player, Vec3d hitPos, Hand hand) {
        player.inventory.clear();

        switch (this.type) {
            case ARCHER:
                this.giveArcherKit(player);
                break;
            case SOLDIER:
                this.giveSoldierKit(player);
                break;
            case CONSTRUCTOR:
                this.giveConstructorKit(player);
                break;
            case SHIELD_BEARER:
                this.giveShieldKit(player);
                break;
        }

        return ActionResult.SUCCESS;
    }

    // give{x}Equipment is for armour stands and players

    private void giveArcherEquipment(LivingEntity entity) {
        entity.equipStack(EquipmentSlot.HEAD, ItemStackBuilder.of(Items.LEATHER_HELMET).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.CHEST, ItemStackBuilder.of(Items.LEATHER_CHESTPLATE).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.LEGS, ItemStackBuilder.of(Items.LEATHER_LEGGINGS).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.FEET, ItemStackBuilder.of(Items.LEATHER_BOOTS).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
    }

    private void giveSoldierEquipment(LivingEntity entity) {
        entity.equipStack(EquipmentSlot.HEAD, ItemStackBuilder.of(Items.LEATHER_HELMET).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.CHEST, ItemStackBuilder.of(Items.LEATHER_CHESTPLATE).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.LEGS, ItemStackBuilder.of(Items.LEATHER_LEGGINGS).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.FEET, ItemStackBuilder.of(Items.LEATHER_BOOTS).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
    }

    private void giveConstructorEquipment(LivingEntity entity) {
        entity.equipStack(EquipmentSlot.HEAD, ItemStackBuilder.of(Items.LEATHER_HELMET).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.CHEST, ItemStackBuilder.of(Items.LEATHER_CHESTPLATE).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.LEGS, ItemStackBuilder.of(Items.LEATHER_LEGGINGS).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.FEET, ItemStackBuilder.of(Items.LEATHER_BOOTS).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_AXE));
        entity.equipStack(EquipmentSlot.OFFHAND, new ItemStack(this.planksForTeam()));
    }

    private void giveShieldEquipment(LivingEntity entity) {
        entity.equipStack(EquipmentSlot.HEAD, ItemStackBuilder.of(Items.LEATHER_HELMET).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.CHEST, ItemStackBuilder.of(Items.LEATHER_CHESTPLATE).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.LEGS, ItemStackBuilder.of(Items.LEATHER_LEGGINGS).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.FEET, ItemStackBuilder.of(Items.LEATHER_BOOTS).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        entity.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
    }

    // give{x}Kit is for players only

    private void giveArcherKit(PlayerEntity player) {

    }

    private void giveSoldierKit(PlayerEntity player) {

    }

    private void giveConstructorKit(PlayerEntity player) {

    }

    private void giveShieldKit(PlayerEntity player) {

    }


    public enum KitType {
        ARCHER(new LiteralText("Archer Kit")),
        SOLDIER(new LiteralText("Soldier Kit")),
        CONSTRUCTOR(new LiteralText("Constructor Kit")),
        SHIELD_BEARER(new LiteralText("Shield Bearer Kit"));

        private final Text name;

        KitType(Text name) {
            this.name = name;
        }
    }
}
