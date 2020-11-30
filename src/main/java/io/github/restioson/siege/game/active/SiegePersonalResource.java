package io.github.restioson.siege.game.active;

public enum SiegePersonalResource {
    WOOD(256);

    public final int max;

    SiegePersonalResource(int max) {
        this.max = max;
    }
}
