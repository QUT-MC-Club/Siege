package io.github.restioson.siege.game.active;

import io.github.restioson.siege.game.SiegeKit;
import io.github.restioson.siege.game.map.SiegeSpawn;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.util.PlayerRef;

public class WarpingPlayer {
    final PlayerRef player;
    final Vec3d pos;
    final SiegeSpawn destination;
    final long startTime;
    @Nullable
    final SiegeKit newKit;

    public WarpingPlayer(ServerPlayerEntity player, SiegeSpawn destination, long time, @Nullable SiegeKit newKit) {
        this.player = PlayerRef.of(player);
        this.pos = player.getPos();
        this.destination = destination;
        this.startTime = time;
        this.newKit = newKit;
    }
}
