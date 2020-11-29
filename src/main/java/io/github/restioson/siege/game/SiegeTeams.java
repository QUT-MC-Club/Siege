package io.github.restioson.siege.game;

import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.DyeColor;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.player.GameTeam;

public final class SiegeTeams implements AutoCloseable {
    public static final GameTeam ATTACKERS = new GameTeam("attackers", "Attackers", DyeColor.RED);
    public static final GameTeam DEFENDERS = new GameTeam("defenders", "Defenders", DyeColor.CYAN);

    private final ServerScoreboard scoreboard;

    final Team attackers;
    final Team defenders;

    public SiegeTeams(GameSpace gameSpace) {
        this.scoreboard = gameSpace.getServer().getScoreboard();

        this.attackers = this.createTeam(ATTACKERS);
        this.defenders = this.createTeam(DEFENDERS);
    }

    @Nullable
    public static GameTeam byKey(String name) {
        switch (name) {
            case "attackers": return ATTACKERS;
            case "defenders": return DEFENDERS;
            default: return null;
        }
    }

    private Team createTeam(GameTeam team) {
        Team scoreboardTeam = this.scoreboard.addTeam(RandomStringUtils.randomAlphanumeric(16));
        scoreboardTeam.setDisplayName(new LiteralText(team.getDisplay()).formatted(team.getFormatting()));
        scoreboardTeam.setColor(team.getFormatting());
        scoreboardTeam.setFriendlyFireAllowed(false);
        scoreboardTeam.setCollisionRule(AbstractTeam.CollisionRule.NEVER);
        return scoreboardTeam;
    }

    public void addPlayer(ServerPlayerEntity player, GameTeam team) {
        Team scoreboardTeam = this.getScoreboardTeam(team);
        this.scoreboard.addPlayerToTeam(player.getEntityName(), scoreboardTeam);
    }

    private Team getScoreboardTeam(GameTeam team) {
        return team == ATTACKERS ? this.attackers : this.defenders;
    }

    @Override
    public void close() {
        this.scoreboard.removeTeam(this.attackers);
        this.scoreboard.removeTeam(this.defenders);
    }
}
