package io.github.restioson.siege.game;

import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.util.BlockBounds;

import java.util.List;

public class SiegeFlag {
    public GameTeam team;
    public BlockBounds bounds;
    public String name;

    public CapturingState capturingState;
    public int captureProgressTicks;

    // The flags which must be captured before this flag can be captured
    public List<SiegeFlag> prerequisiteFlags;

    public SiegeFlag(GameTeam team, BlockBounds bounds, String name, List<SiegeFlag> prerequisiteFlags) {
        this.team = team;
        this.bounds = bounds;
        this.name = name;
        this.prerequisiteFlags = prerequisiteFlags;
    }

    boolean incrementCapture(GameTeam team, int amount) {
        this.captureProgressTicks += amount;

        if (this.captureProgressTicks >= SiegeCaptureLogic.CAPTURE_TIME_TICKS) {
            this.captureProgressTicks = 0;
            this.team = team;
            this.capturingState = null;
            return true;
        }

        return false;
    }

    boolean decrementCapture(int amount) {
        this.captureProgressTicks -= amount;

        if (this.captureProgressTicks <= 0) {
            this.captureProgressTicks = 0;
            this.capturingState = null;
            return true;
        }

        return false;
    }
}
