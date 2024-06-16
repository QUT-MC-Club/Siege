package io.github.restioson.siege.entity;

import io.github.restioson.siege.game.SiegeKit;
import io.github.restioson.siege.game.active.SiegeActive;
import io.github.restioson.siege.game.active.SiegePlayer;
import io.github.restioson.siege.game.map.SiegeFlag;
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
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;

public final class SiegeKitStandEntity extends ArmorStandEntity {
    @Nullable
    public final SiegeFlag controllingFlag;
    @Nullable
    private final GameTeam team;

    private final SiegeKit kit;
    private final SiegeActive game;

    public SiegeKitStandEntity(World world, SiegeActive game, SiegeKitStandLocation stand) {
        super(EntityType.ARMOR_STAND, world);
        this.kit = stand.type();
        this.controllingFlag = stand.flag();
        this.team = this.controllingFlag != null ? this.controllingFlag.team : stand.team();
        this.game = game;
        this.setPose(EntityPose.CROUCHING);

        this.updatePositionAndAngles(stand.pos().x, stand.pos().y, stand.pos().z, stand.yaw(), 0);

        this.setCustomName(this.kit.name());
        this.setInvulnerable(true);
        this.setCustomNameVisible(true);
        this.setShowArms(true);
        this.kit.equipArmourStand(this);
    }

    public GameTeam getTeam() {
        return this.controllingFlag != null ? this.controllingFlag.team : this.team;
    }

    public void onControllingFlagCaptured() {
        this.kit.equipArmourStand(this);
    }

    @Override
    public ActionResult interactAt(PlayerEntity playerEntity, Vec3d hitPos, Hand hand) {
        var player = (ServerPlayerEntity) playerEntity;
        SiegePlayer participant = this.game.participant(player);

        if (participant == null || player.interactionManager.getGameMode() != GameMode.SURVIVAL) {
            return ActionResult.FAIL;
        }

        if (participant.team != this.getTeam()) {
            return ActionResult.FAIL;
        }

        participant.kit.returnResources(player, participant);
        participant.kit = this.kit;
        this.kit.equipPlayer(player, participant, this.game.config, player.getWorld().getTime());
        return ActionResult.SUCCESS;
    }

    @Override
    public boolean isImmobile() {
        return true;
    }

    @Override
    public boolean isMarker() {
        return true;
    }
}
