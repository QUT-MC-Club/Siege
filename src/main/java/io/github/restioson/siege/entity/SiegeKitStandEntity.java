package io.github.restioson.siege.entity;

import io.github.restioson.siege.game.SiegeKit;
import io.github.restioson.siege.game.active.SiegeActive;
import io.github.restioson.siege.game.active.SiegePlayer;
import io.github.restioson.siege.game.map.SiegeFlag;
import io.github.restioson.siege.game.map.SiegeKitStandData;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;

public final class SiegeKitStandEntity extends ArmorStandEntity {
    @Nullable
    public final SiegeFlag controllingFlag;
    @Nullable
    private final GameTeam team;
    private final SiegeKitStandData data;

    private final SiegeKit kit;
    private final SiegeActive game;

    public SiegeKitStandEntity(SiegeActive game, SiegeKitStandData stand) {
        super(EntityType.ARMOR_STAND, game.world);
        this.kit = stand.type();
        this.controllingFlag = stand.flag();
        this.team = this.controllingFlag != null ? this.controllingFlag.team : stand.team();
        this.game = game;
        this.setPose(EntityPose.CROUCHING);
        this.data = stand;

        this.updatePositionAndAngles(stand.pos().x, stand.pos().y, stand.pos().z, stand.yaw(), 0);

        this.setCustomName(this.kit.getName());
        this.setInvulnerable(true);
        this.setCustomNameVisible(true);
        this.setShowArms(true);
        this.kit.equipArmourStand(this);
    }

    public GameTeam getTeam() {
        return this.controllingFlag != null ? this.controllingFlag.team : this.team;
    }

    public void onControllingFlagCaptured() {
        // TODO HACK: viaversion does not work with just equipStack
        this.remove(RemovalReason.DISCARDED);
        this.game.world.spawnEntity(new SiegeKitStandEntity(this.game, this.data));
    }

    @Override
    public ActionResult interactAt(PlayerEntity playerEntity, Vec3d hitPos, Hand hand) {
        var player = (ServerPlayerEntity) playerEntity;
        SiegePlayer participant = this.game.participant(player);

        if (participant == null || player.interactionManager.getGameMode() != GameMode.SURVIVAL) {
            return ActionResult.FAIL;
        }

        if (participant.team != this.getTeam()) {
            player.sendMessage(Text.translatable("game.siege.kit.not_your_stand").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        var cooldownMgr = player.getItemCooldownManager();
        if (cooldownMgr.isCoolingDown(SiegeKit.KIT_SELECT_ITEM)) {
            player.sendMessage(Text.translatable("game.siege.kit.cooldown").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        this.kit.equipPlayer(player, participant, this.game.config, player.getWorld().getTime());
        cooldownMgr.set(SiegeKit.KIT_SELECT_ITEM, SiegeKit.KIT_SWAP_COOLDOWN);

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
