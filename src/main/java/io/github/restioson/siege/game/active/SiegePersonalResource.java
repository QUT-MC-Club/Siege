package io.github.restioson.siege.game.active;

public enum SiegePersonalResource {
    WOOD(64, 2),
    TNT(4, 30);

    public final int max;
    public final int refreshSecs;

    SiegePersonalResource(int max, int refreshSecs) {
        this.max = max;
        this.refreshSecs = refreshSecs;
    }

    public int getNextRefreshSecs(long time) {
        return this.refreshSecs - (int) ((time % (this.refreshSecs * 20L)) / 20);
    }
}
