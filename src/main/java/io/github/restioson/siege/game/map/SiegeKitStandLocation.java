package io.github.restioson.siege.game.map;

import io.github.restioson.siege.game.SiegeKit;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.plasmid.game.player.GameTeam;

public class SiegeKitStandLocation {
    public final GameTeam team;
    public final Vec3d pos;
    public final float yaw;
    public final SiegeKit type;

    public SiegeKitStandLocation(GameTeam team, Vec3d pos, SiegeKit type, float yaw) {
        this.team = team;
        this.pos = pos;
        this.type = type;
        this.yaw = yaw;
    }
}
