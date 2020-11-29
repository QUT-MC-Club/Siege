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

        this.updatePositionAndAngles(stand.pos.x, stand.pos.y, stand.pos.z, stand.yaw, 0);

        this.setCustomName(this.type.name);
        this.setInvulnerable(true);
        this.setCustomNameVisible(true);
        this.setShowArms(true);

        switch (this.type) {
            case ARCHER:
                this.giveArcherEquipment(this);
                this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.WOODEN_SWORD));
                this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                break;
            case SOLDIER:
                this.giveSoldierEquipment(this);
                this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.IRON_AXE));
                break;
            case CONSTRUCTOR:
                this.giveConstructorEquipment(this);
                this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_AXE));
                break;
            case SHIELD_BEARER:
                this.giveShieldEquipment(this);
                this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
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

        player.inventory.insertStack(ItemStackBuilder.of(Items.COOKED_BEEF).setCount(5).build());

        return ActionResult.SUCCESS;
    }

    // give{x}Equipment is for armour stands and players

    private void giveArcherEquipment(LivingEntity entity) {
        entity.equipStack(EquipmentSlot.HEAD, ItemStackBuilder.of(Items.LEATHER_HELMET).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.CHEST, ItemStackBuilder.of(Items.LEATHER_CHESTPLATE).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.LEGS, ItemStackBuilder.of(Items.LEATHER_LEGGINGS).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.FEET, ItemStackBuilder.of(Items.LEATHER_BOOTS).setColor(this.team.getColor()).build());
    }

    private void giveSoldierEquipment(LivingEntity entity) {
        entity.equipStack(EquipmentSlot.HEAD, ItemStackBuilder.of(Items.LEATHER_HELMET).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.CHEST, ItemStackBuilder.of(Items.IRON_CHESTPLATE).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.LEGS, ItemStackBuilder.of(Items.LEATHER_LEGGINGS).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.FEET, ItemStackBuilder.of(Items.IRON_BOOTS).setColor(this.team.getColor()).build());
    }

    private void giveConstructorEquipment(LivingEntity entity) {
        entity.equipStack(EquipmentSlot.HEAD, ItemStackBuilder.of(Items.LEATHER_HELMET).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.CHEST, ItemStackBuilder.of(Items.LEATHER_CHESTPLATE).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.LEGS, ItemStackBuilder.of(Items.LEATHER_LEGGINGS).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.FEET, ItemStackBuilder.of(Items.LEATHER_BOOTS).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.OFFHAND, new ItemStack(this.planksForTeam()));
    }

    private void giveShieldEquipment(LivingEntity entity) {
        entity.equipStack(EquipmentSlot.HEAD, ItemStackBuilder.of(Items.LEATHER_HELMET).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.CHEST, ItemStackBuilder.of(Items.IRON_CHESTPLATE).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.LEGS, ItemStackBuilder.of(Items.LEATHER_LEGGINGS).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.FEET, ItemStackBuilder.of(Items.LEATHER_BOOTS).setColor(this.team.getColor()).build());
        entity.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
    }

    // give{x}Kit is for players only

    private void giveArcherKit(PlayerEntity player) {
        this.giveArcherEquipment(player);
        player.inventory.insertStack(new ItemStack(Items.WOODEN_SWORD));
        player.inventory.insertStack(new ItemStack(Items.BOW));
        player.inventory.insertStack(ItemStackBuilder.of(Items.ARROW).setCount(32).build());
    }

    private void giveSoldierKit(PlayerEntity player) {
        this.giveShieldEquipment(player);
        player.inventory.insertStack(new ItemStack(Items.IRON_SWORD));
        player.inventory.insertStack(new ItemStack(Items.IRON_AXE));
    }

    private void giveConstructorKit(PlayerEntity player) {
        this.giveConstructorEquipment(player);
        player.equipStack(EquipmentSlot.OFFHAND, ItemStackBuilder.of(this.planksForTeam()).setCount(16).build());
        player.inventory.insertStack(new ItemStack(Items.WOODEN_SWORD));
        player.inventory.insertStack(new ItemStack(Items.WOODEN_AXE));
        player.inventory.insertStack(ItemStackBuilder.of(Items.LADDER).setCount(4).build());
    }

    private void giveShieldKit(PlayerEntity player) {
        this.giveShieldEquipment(player);
        player.inventory.insertStack(new ItemStack(Items.IRON_SWORD));
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
