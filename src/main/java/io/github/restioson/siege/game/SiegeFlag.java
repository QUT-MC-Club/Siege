package io.github.restioson.siege.game;


import xyz.nucleoid.plasmid.util.BlockBounds;

import java.util.List;

public class SiegeFlag {
    public SiegeTeam team;
    public BlockBounds bounds;
    public String name;
    public int captureProgressTicks;

    // The flags which must be captured before this flag can be captured
    public List<SiegeFlag> prerequisiteFlags;

    public SiegeFlag(SiegeTeam team, BlockBounds bounds, String name, List<SiegeFlag> prerequisiteFlags) {
        this.team = team;
        this.bounds = bounds;
        this.name = name;
        this.prerequisiteFlags = prerequisiteFlags;
    }
}
