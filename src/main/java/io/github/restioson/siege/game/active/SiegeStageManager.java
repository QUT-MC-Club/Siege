package io.github.restioson.siege.game.active;

import io.github.restioson.siege.game.SiegeTeams;
import io.github.restioson.siege.game.map.SiegeFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;

public class SiegeStageManager {
    private final SiegeActive game;
    private final boolean singlePlayer;

    private long closeTime = -1;
    public long startTime = -1;

    SiegeStageManager(SiegeActive game) {
        this.game = game;
        this.singlePlayer = game.gameSpace.getPlayers().size() <= 1;
    }

    public void onOpen(long time) {
        this.startTime = time;
    }

    public long finishTime() {
        return this.startTime + (this.game.timeLimitSecs() * 20);
    }

    public TickResult tick(long time) {
        if (this.game.gameSpace.getPlayers().isEmpty()) {
            return TickResult.GAME_CLOSED;
        }

        // Game has finished. Wait a few seconds before finally closing the game.
        if (this.closeTime > 0) {
            return this.tickClosing(time);
        }

        if (this.testDefendersWin(time)) {
            this.triggerFinish(time);
            return TickResult.DEFENDERS_WIN;
        }

        if (this.testAttackersWin()) {
            this.triggerFinish(time);
            return TickResult.ATTACKERS_WIN;
        }

        if (!this.singlePlayer && this.game.gameSpace.getPlayers().size() <= 1) {
            GameTeam team = this.getRemainingTeam();
            this.triggerFinish(time);
            if (team == SiegeTeams.DEFENDERS) {
                return TickResult.DEFENDERS_WIN;
            } else {
                return TickResult.ATTACKERS_WIN;
            }
        }

        var timeToFinish = this.finishTime() - time;
        if (timeToFinish == 20 * 60) {
            SiegeDialogueLogic.broadcastTimeRunningOut(this.game);
        }

        return TickResult.CONTINUE_TICK;
    }

    private TickResult tickClosing(long time) {
        if (time >= this.closeTime) {
            return TickResult.GAME_CLOSED;
        }
        return TickResult.TICK_FINISHED;
    }

    private void triggerFinish(long time) {
        for (ServerPlayerEntity player : this.game.gameSpace.getPlayers()) {
            player.changeGameMode(GameMode.SPECTATOR);
        }

        this.closeTime = time + (15 * 20);
    }

    private boolean testDefendersWin(long time) {
        return time >= this.finishTime();
    }

    private boolean testAttackersWin() {
        for (SiegeFlag flag : this.game.map.flags) {
            if (flag.team != SiegeTeams.ATTACKERS) {
                return false;
            }
        }
        return true;
    }

    private GameTeam getRemainingTeam() {
        for (ServerPlayerEntity player : this.game.gameSpace.getPlayers()) {
            SiegePlayer participant = this.game.participant(player);
            if (participant != null) {
                return participant.team;
            }
        }
        return SiegeTeams.DEFENDERS;
    }

    public enum TickResult {
        CONTINUE_TICK,
        TICK_FINISHED,
        ATTACKERS_WIN,
        DEFENDERS_WIN,
        GAME_CLOSED,
    }
}
