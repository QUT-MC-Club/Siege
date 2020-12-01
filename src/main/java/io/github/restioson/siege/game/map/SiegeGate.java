package io.github.restioson.siege.game.map;

import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.util.BlockBounds;

public class SiegeGate {
    public BlockBounds portcullis;
    public BlockBounds gateOpen;
    public int openProgress;
    public GameTeam team;

    public SiegeGate(BlockBounds gateOpen, GameTeam team) {
        this.gateOpen = gateOpen;
        this.team = team;
        this.openProgress = 0;
    }
}
