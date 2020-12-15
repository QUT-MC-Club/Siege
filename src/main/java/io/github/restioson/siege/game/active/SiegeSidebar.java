package io.github.restioson.siege.game.active;

import io.github.restioson.siege.game.SiegeTeams;
import io.github.restioson.siege.game.map.SiegeFlag;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.widget.GlobalWidgets;
import xyz.nucleoid.plasmid.widget.SidebarWidget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SiegeSidebar {
    private static final int SORT_CAPTURING = 0;
    private static final int SORT_ATTACKERS = 1;
    private static final int SORT_DEFENDERS = 2;

    private final SiegeActive game;
    private final SidebarWidget widget;

    SiegeSidebar(SiegeActive game, GlobalWidgets widgets) {
        this.game = game;
        this.widget = widgets.addSidebar(new LiteralText("Siege").formatted(Formatting.BOLD, Formatting.GOLD));
    }

    public void update(long time) {
        this.widget.set(content -> {
            List<SiegeFlag> flags = new ArrayList<>(this.game.map.flags);
            flags.sort(Comparator.comparingInt(SiegeSidebar::getSortIndex));

            long seconds = time / 20;
            boolean blink = seconds % 2 == 0;

            for (SiegeFlag flag : flags) {
                if (!flag.capturable) {
                    continue;
                }

                Formatting color = flag.team.getFormatting();
                boolean capturing = false;
                boolean italic = false;

                if (flag.capturingState != null && flag.capturingState.hasAlert()) {
                    if (flag.capturingState == CapturingState.CONTESTED) {
                        color = blink ? color : Formatting.GRAY;
                    } else {
                        GameTeam blinkTeam = blink ? SiegeTeams.ATTACKERS : SiegeTeams.DEFENDERS;
                        color = blinkTeam.getFormatting();
                        italic = blinkTeam != flag.team;
                    }
                    capturing = true;
                }

                String line;
                if (italic) {
                    line = color + Formatting.ITALIC.toString() + flag.name;
                } else {
                    line = color + flag.name;
                }

                if (capturing) line = "(!) " + line;

                content.writeLine(line);
            }
        });
    }

    private static int getSortIndex(SiegeFlag flag) {
        if (flag.capturingState != null) {
            return SORT_CAPTURING;
        } else if (flag.team == SiegeTeams.ATTACKERS) {
            return SORT_ATTACKERS;
        } else if (flag.team == SiegeTeams.DEFENDERS) {
            return SORT_DEFENDERS;
        }
        return -1;
    }
}
