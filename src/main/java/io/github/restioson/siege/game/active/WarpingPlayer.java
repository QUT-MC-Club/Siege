package io.github.restioson.siege.game.active;

import io.github.restioson.siege.game.map.SiegeFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.plasmid.util.PlayerRef;

public class WarpingPlayer {
    PlayerRef player;
    BlockPos pos;
    SiegeFlag destination;
    long startTime;

    public WarpingPlayer(ServerPlayerEntity player, SiegeFlag destination, long time) {
        this.player = PlayerRef.of(player);
        this.pos = player.getBlockPos();
        this.startTime = time;
    }
}
