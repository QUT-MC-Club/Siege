package io.github.restioson.siege.game.active;

import io.github.restioson.siege.game.SiegeKit;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.player.GameTeam;

public class SiegePlayer {
    public GameTeam team;
    public SiegeKit kit;
    @Nullable
    public AttackRecord lastTimeWasAttacked;

    public SiegePlayer(GameTeam team) {
        this.team = team;
        this.kit = SiegeKit.SOLDIER;
    }

    public ServerPlayerEntity attacker(long time, ServerWorld world) {
        if (this.lastTimeWasAttacked != null) {
            return this.lastTimeWasAttacked.isValid(time) ? this.lastTimeWasAttacked.player.getEntity(world) : null;
        } else {
            return null;
        }
    }
}
