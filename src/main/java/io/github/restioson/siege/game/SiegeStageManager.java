package io.github.restioson.siege.game;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameSpace;

public class SiegeStageManager {
    private long closeTime = -1;
    public long finishTime = -1;
    private boolean setSpectator = false;

    public void onOpen(long time, SiegeConfig config) {
        this.finishTime = time + (config.timeLimitMins * 20 * 60);
    }

    public TickResult tick(long time, GameSpace gameSpace) {
        // Game has finished. Wait a few seconds before finally closing the game.
        if (this.closeTime > 0) {
            if (time >= this.closeTime) {
                return TickResult.GAME_CLOSED;
            }
            return TickResult.TICK_FINISHED;
        }

        // Game has just finished. Transition to the waiting-before-close state.
        if (time > this.finishTime || gameSpace.getPlayers().isEmpty()) {
            if (!this.setSpectator) {
                this.setSpectator = true;
                for (ServerPlayerEntity player : gameSpace.getPlayers()) {
                    player.setGameMode(GameMode.SPECTATOR);
                }
            }

            this.closeTime = time + (5 * 20);

            return TickResult.GAME_FINISHED;
        }

        return TickResult.CONTINUE_TICK;
    }

    public enum TickResult {
        CONTINUE_TICK,
        TICK_FINISHED,
        GAME_FINISHED,
        GAME_CLOSED,
    }
}
