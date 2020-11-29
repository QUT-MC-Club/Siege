package io.github.restioson.siege.game.map;

import io.github.restioson.siege.game.SiegeKitStandEntity;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.plasmid.game.player.GameTeam;

public class SiegeKitStandLocation {
    public final GameTeam team;
    public final Vec3d pos;
    public final SiegeKitStandEntity.KitType type;

    public SiegeKitStandLocation(GameTeam team, Vec3d pos, SiegeKitStandEntity.KitType type) {
        this.team = team;
        this.pos = pos;
        this.type = type;
    }
}
