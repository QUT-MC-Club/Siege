package io.github.restioson.siege.game;

import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.common.team.*;

import java.util.List;

public final class SiegeTeams {
    public static final GameTeam ATTACKERS = new GameTeam(
            new GameTeamKey("attackers"),
            GameTeamConfig.builder()
                    .setName(Text.literal("Attackers"))
                    .setColors(GameTeamConfig.Colors.from(DyeColor.RED))
                    .setCollision(AbstractTeam.CollisionRule.NEVER)
                    .setFriendlyFire(false)
                    .build()
    );
    public static final GameTeam DEFENDERS = new GameTeam(
            new GameTeamKey("defenders"),
            GameTeamConfig.builder()
                    .setName(Text.literal("Defenders"))
                    .setColors(GameTeamConfig.Colors.from(DyeColor.BLUE))
                    .setCollision(AbstractTeam.CollisionRule.NEVER)
                    .setFriendlyFire(false)
                    .build()
    );

    public static final GameTeamList TEAMS = new GameTeamList(List.of(ATTACKERS, DEFENDERS));

    private final TeamManager teams;

    public SiegeTeams(GameActivity activity) {
        this.teams = TeamManager.addTo(activity);
        TeamChat.addTo(activity, this.teams);
        this.teams.addTeams(TEAMS);
    }

    public static GameTeam opposite(GameTeam team) {
        return team == ATTACKERS ? DEFENDERS : ATTACKERS;
    }

    public static GameTeam byKey(GameTeamKey team) {
        return team == ATTACKERS.key() ? ATTACKERS : DEFENDERS;
    }

    @Nullable
    public static GameTeam byKey(String name) {
        return switch (name) {
            case "attackers" -> ATTACKERS;
            case "defenders" -> DEFENDERS;
            default -> null;
        };
    }

    public void addPlayer(ServerPlayerEntity player, GameTeamKey team) {
        this.teams.addPlayerTo(player, team);
    }

    public void removePlayer(ServerPlayerEntity player, GameTeamKey team) {
        this.teams.removePlayerFrom(player, team);
    }

    public GameTeamKey getSmallestTeam() {
        return this.teams.getSmallestTeam();
    }
}
