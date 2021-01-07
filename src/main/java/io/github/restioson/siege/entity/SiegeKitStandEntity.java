package io.github.restioson.siege.entity;

import io.github.restioson.siege.game.SiegeKit;
import io.github.restioson.siege.game.active.SiegeActive;
import io.github.restioson.siege.game.active.SiegePlayer;
import io.github.restioson.siege.game.map.SiegeKitStandLocation;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import xyz.nucleoid.plasmid.game.player.GameTeam;

public final class SiegeKitStandEntity extends ArmorStandEntity {
    public final GameTeam team;
    private final SiegeKit type;
    private final SiegeActive game;

    public SiegeKitStandEntity(World world, SiegeActive game, SiegeKitStandLocation stand) {
        super(EntityType.ARMOR_STAND, world);
        this.type = stand.type;
        this.team = stand.team;
        this.game = game;
        this.setPose(EntityPose.CROUCHING);

        this.updatePositionAndAngles(stand.pos.x, stand.pos.y, stand.pos.z, stand.yaw, 0);

        this.setCustomName(this.type.name);
        this.setInvulnerable(true);
        this.setCustomNameVisible(true);
        this.setShowArms(true);
        this.type.equipArmourStand(this);
    }

    @Override
    public ActionResult interactAt(PlayerEntity player, Vec3d hitPos, Hand hand) {
        SiegePlayer participant = this.game.participant((ServerPlayerEntity) player);
        if (participant == null || ((ServerPlayerEntity) player).interactionManager.getGameMode() != GameMode.SURVIVAL) {
            return ActionResult.FAIL;
        }

        if (participant.team != this.team) {
            return ActionResult.FAIL;
        }

        participant.kit = this.type;
        this.type.equipPlayer((ServerPlayerEntity) player, participant, this.game.gameSpace.getWorld(), this.game.config);
        return ActionResult.SUCCESS;
    }

    @Override
    public boolean isImmobile() {
        return true;
    }
}
