package io.github.restioson.siege.game;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.DyeColor;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.common.team.*;

import java.util.List;

public final class SiegeTeams {
    public static final GameTeam ATTACKERS = new GameTeam(
            new GameTeamKey("attackers"),
            GameTeamConfig.builder()
                    .setName(new LiteralText("Attackers"))
                    .setColors(GameTeamConfig.Colors.from(DyeColor.RED))
                    .setCollision(AbstractTeam.CollisionRule.NEVER)
                    .setFriendlyFire(false)
                    .build()
    );
    public static final GameTeam DEFENDERS = new GameTeam(
            new GameTeamKey("defenders"),
            GameTeamConfig.builder()
                    .setName(new LiteralText("Defenders"))
                    .setColors(GameTeamConfig.Colors.from(DyeColor.CYAN))
                    .setCollision(AbstractTeam.CollisionRule.NEVER)
                    .setFriendlyFire(false)
                    .build()
    );

    public static final GameTeamList TEAMS = new GameTeamList(List.of(ATTACKERS, DEFENDERS));

    private final TeamManager teams;

    public SiegeTeams(GameActivity activity) {
        this.teams = TeamManager.addTo(activity);
        this.teams.addTeams(TEAMS);
    }

    public static GameTeam byKey(GameTeamKey team) {
        return team == ATTACKERS.key() ? ATTACKERS : DEFENDERS;
    }

    public static Item planksForTeam(GameTeamKey team) {
        if (team == SiegeTeams.ATTACKERS.key()) {
            return Items.ACACIA_PLANKS;
        } else {
            return Items.BIRCH_PLANKS;
        }
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
