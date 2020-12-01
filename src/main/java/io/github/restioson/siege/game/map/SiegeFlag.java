package io.github.restioson.siege.game.map;

import io.github.restioson.siege.game.active.CapturingState;
import io.github.restioson.siege.game.active.SiegeCaptureLogic;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.util.BlockBounds;

import java.util.ArrayList;
import java.util.List;

public final class SiegeFlag {
    public final String id;
    public final String name;
    public final BlockBounds bounds;
    public boolean capturable = true;
    public boolean pluralName = false;
    @Nullable
    public BlockBounds flagIndicatorBlocks;
    public final List<SiegeGate> attachedGates = new ArrayList<>();

    @Nullable
    public BlockBounds respawn;

    public GameTeam team;

    public CapturingState capturingState;
    public int captureProgressTicks;

    // The flags which must be captured before this flag can be captured
    public List<SiegeFlag> prerequisiteFlags = new ArrayList<>();

    public SiegeFlag(String id, String name, GameTeam team, BlockBounds bounds) {
        this.id = id;
        this.name = name;
        this.team = team;
        this.bounds = bounds;
    }

    public String pastToBe() {
        if (this.pluralName) {
            return "have";
        } else {
            return "has";
        }
    }

    public String presentTobe() {
        if (this.pluralName) {
            return "are";
        } else {
            return "is";
        }
    }

    public boolean incrementCapture(GameTeam team, int amount) {
        this.captureProgressTicks += amount;

        if (this.captureProgressTicks >= SiegeCaptureLogic.CAPTURE_TIME_TICKS) {
            this.captureProgressTicks = 0;
            this.team = team;
            this.capturingState = null;
            return true;
        }

        return false;
    }

    public boolean decrementCapture(int amount) {
        this.captureProgressTicks -= amount;

        if (this.captureProgressTicks <= 0) {
            this.captureProgressTicks = 0;
            this.capturingState = null;
            return true;
        }

        return false;
    }
}
