package io.github.restioson.siege.game.map;

import xyz.nucleoid.plasmid.util.BlockBounds;

public final class SiegeSpawn {
    public final BlockBounds bounds;
    public final float yaw;

    public SiegeSpawn(BlockBounds bounds, float yaw) {
        this.bounds = bounds;
        this.yaw = yaw;
    }
}
