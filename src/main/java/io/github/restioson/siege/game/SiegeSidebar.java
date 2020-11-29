package io.github.restioson.siege.game;

import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.widget.GlobalWidgets;
import xyz.nucleoid.plasmid.widget.SidebarWidget;

public final class SiegeSidebar {
    private final SiegeActive game;
    private final SidebarWidget widget;

    SiegeSidebar(SiegeActive game, GlobalWidgets widgets) {
        this.game = game;
        this.widget = widgets.addSidebar(new LiteralText("Siege").formatted(Formatting.BOLD, Formatting.GOLD));
    }

    public void update(long time) {
        this.widget.set(content -> {
            long seconds = time / 20;
            boolean blink = seconds % 2 == 0;

            for (SiegeFlag flag : this.game.map.flags) {
                Formatting color = flag.team.getFormatting();
                boolean capturing = false;
                if (flag.capturingState != null) {
                    if (flag.capturingState == CapturingState.CONTESTED) {
                        color = blink ? color : Formatting.GRAY;
                    } else {
                        color = blink ? SiegeTeams.ATTACKERS.getFormatting() : SiegeTeams.DEFENDERS.getFormatting();
                    }
                    capturing = true;
                }

                if (capturing) {
                    content.writeLine("(!) " + color + flag.name);
                } else {
                    content.writeLine(color + flag.name);
                }
            }
        });
    }
}
