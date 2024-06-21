package io.github.restioson.siege.game.active;

import io.github.restioson.siege.game.SiegeTeams;
import io.github.restioson.siege.game.map.SiegeFlag;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;

public class SiegeStageManager {
    private final SiegeActive game;
    private final boolean singlePlayer;

    public final ServerBossBar timerBar = new ServerBossBar(
            Text.translatable("game.siege.timer.time_left"),
            BossBar.Color.BLUE,
            BossBar.Style.PROGRESS
    );
    private long closeTime = -1;
    public long startTime = -1;
    private long maxPotentialTime = -1;
    private long finishTime = -1;

    SiegeStageManager(SiegeActive game) {
        this.game = game;
        this.singlePlayer = game.gameSpace.getPlayers().size() <= 1;
    }

    public void onOpen(long time) {
        this.startTime = time;
        this.maxPotentialTime = this.game.config.timeLimitMins() * 60L * 20L;
        this.finishTime = this.startTime + this.maxPotentialTime;
    }

    public TickResult tick(long time) {
        if (this.game.gameSpace.getPlayers().isEmpty()) {
            return TickResult.GAME_CLOSED;
        }

        // Game has finished. Wait a few seconds before finally closing the game.
        if (this.closeTime > 0) {
            return this.tickClosing(time);
        }

        if (this.testOvertime(time)) {
            this.timerBar.setName(Text.translatable("game.siege.timer.overtime").formatted(Formatting.RED));
            this.timerBar.setPercent(1.0f);
            return TickResult.OVERTIME;
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

        var timeToFinish = this.finishTime - time;
        if (timeToFinish == 20 * 60) {
            SiegeDialogueLogic.broadcastTimeRunningOut(this.game);
        }

        long ticksTillEnd = this.finishTime - time;
        long secondsUntilEnd = ticksTillEnd / 20;
        long minutes = secondsUntilEnd / 60;
        long seconds = secondsUntilEnd % 60;
        var timerBarText = Text.translatable("game.siege.timer.time_left")
                .append(" ")
                .append(
                        Text.literal(String.format("%02d:%02d", minutes, seconds))
                                .formatted(Formatting.AQUA)
                );

        this.timerBar.setName(timerBarText);
        this.timerBar.setPercent((float) ticksTillEnd / this.maxPotentialTime);

        return TickResult.CONTINUE_TICK;
    }

    private boolean testOvertime(long time) {
        return this.game.config.capturingGiveTimeSecs() > 0
                && time >= this.finishTime
                && this.game.map.flags.stream()
                        .anyMatch(flag -> flag.captureProgressTicks > 0 || flag.isFlagUnderAttack());
    }

    private TickResult tickClosing(long time) {
        if (time >= this.closeTime) {
            return TickResult.GAME_CLOSED;
        }
        return TickResult.TICK_FINISHED;
    }

    private void triggerFinish(long time) {
        this.closeTimerBar();

        for (ServerPlayerEntity player : this.game.gameSpace.getPlayers()) {
            player.changeGameMode(GameMode.SPECTATOR);
        }

        this.closeTime = time + (15 * 20);
    }

    private boolean testDefendersWin(long time) {
        return time >= this.finishTime;
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

    public void addTime(int secs) {
        long timeNow = this.game.world.getTime();
        if (timeNow > this.finishTime) {
            this.finishTime = timeNow;
        }

        this.maxPotentialTime += secs * 20L;
        this.finishTime += secs * 20L;
    }

    public void closeTimerBar() {
        this.timerBar.setVisible(false);
        this.timerBar.clearPlayers();
    }

    public enum TickResult {
        CONTINUE_TICK,
        TICK_FINISHED,
        ATTACKERS_WIN,
        DEFENDERS_WIN,
        OVERTIME,
        GAME_CLOSED;

        public boolean continueGame() {
            return this == CONTINUE_TICK || this == OVERTIME;
        }
    }
}
