package io.github.restioson.siege.game.active;

import io.github.restioson.siege.game.SiegeTeams;
import io.github.restioson.siege.game.map.SiegeFlag;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.game.common.widget.SidebarWidget;

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
        this.widget = widgets.addSidebar(Text.literal("Siege").formatted(Formatting.BOLD, Formatting.GOLD));
    }

    public void update(long time) {
        this.widget.set(content -> {
            long ticksUntilEnd = this.game.stageManager.finishTime() - time;
            content.add(getTimeLeft(ticksUntilEnd));
            content.add(ScreenTexts.EMPTY);

            List<SiegeFlag> flags = new ArrayList<>(this.game.map.flags);
            flags.sort(Comparator.comparingInt(SiegeSidebar::getSortIndex));

            long seconds = time / 20;
            boolean blink = seconds % 2 == 0;

            for (SiegeFlag flag : flags) {
                if (!flag.capturable) {
                    continue;
                }

                Formatting color = flag.team.config().chatFormatting();
                boolean capturing = false;
                boolean italic = false;

                if (flag.capturingState != null && flag.capturingState.hasAlert()) {
                    if (flag.capturingState == CapturingState.CONTESTED) {
                        color = blink ? color : Formatting.GRAY;
                    } else {
                        GameTeam blinkTeam = blink ? SiegeTeams.ATTACKERS : SiegeTeams.DEFENDERS;
                        color = blinkTeam.config().chatFormatting();
                        italic = blinkTeam != flag.team;
                    }
                    capturing = true;
                }

                var flagName = Text.literal(flag.name)
                        .formatted(color);
                if (italic) flagName = flagName.formatted(Formatting.ITALIC);

                float ratio = (float) flag.captureProgressTicks / SiegeCaptureLogic.CAPTURE_TIME_TICKS;
                int percent = (int) Math.floor(ratio * 100);

                Text line;
                if (capturing || percent > 0) {
                    line = Text.literal("(" + percent + "%) ").append(flagName);
                } else if (flag.gateUnderAttack(time)) {
                    line = Text.literal("(!) ").append(flagName);
                } else {
                    line = flagName;
                }

                content.add(line);
            }
        });
    }

    private static Text getTimeLeft(long ticksUntilEnd) {
        long secondsUntilEnd = ticksUntilEnd / 20;

        long minutes = secondsUntilEnd / 60;
        long seconds = secondsUntilEnd % 60;

        return Text.literal("Time Left: ").formatted(Formatting.GOLD)
                .append(Text.literal(String.format("%02d:%02d", minutes, seconds).formatted(Formatting.AQUA)));
    }

    private static int getSortIndex(SiegeFlag flag) {
        if (flag.capturingState != null && flag.capturingState.hasAlert()) {
            return SORT_CAPTURING;
        } else if (flag.team == SiegeTeams.ATTACKERS) {
            return SORT_ATTACKERS;
        } else if (flag.team == SiegeTeams.DEFENDERS) {
            return SORT_DEFENDERS;
        }
        return -1;
    }
}
