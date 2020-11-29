package io.github.restioson.siege.game.active;

import io.github.restioson.siege.game.SiegeKit;
import xyz.nucleoid.plasmid.game.player.GameTeam;

public class SiegePlayer {
    public GameTeam team;
    public SiegeKit kit;

    public SiegePlayer(GameTeam team) {
        this.team = team;
        this.kit = SiegeKit.SOLDIER;
    }
}
