package io.github.restioson.siege.game;

import net.minecraft.util.DyeColor;
import xyz.nucleoid.plasmid.game.player.GameTeam;

public final class SiegeTeams {
    public static final GameTeam ATTACKERS = new GameTeam("attackers", "Attackers", DyeColor.RED);
    public static final GameTeam DEFENDERS = new GameTeam("defenders", "Defenders", DyeColor.CYAN);
}
