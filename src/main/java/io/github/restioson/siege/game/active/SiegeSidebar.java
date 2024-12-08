package io.github.restioson.siege.game.active;

import io.github.restioson.siege.game.SiegeConfig;
import io.github.restioson.siege.game.SiegeTeams;
import io.github.restioson.siege.game.map.SiegeFlag;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.api.game.common.widget.SidebarWidget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static io.github.restioson.siege.game.active.capturing.CapturingStateSidebarBlink.NO_BLINK;

public final class SiegeSidebar {
    private static final int SORT_THREATENED = 0;
    private static final int SORT_ATTACKERS = 1;
    private static final int SORT_DEFENDERS = 2;

    private final SiegeActive game;
    private final SidebarWidget widget;
    private final SiegeConfig config;

    SiegeSidebar(SiegeActive game, GlobalWidgets widgets) {
        this.game = game;
        this.widget = widgets.addSidebar(Text.translatable("gameType.siege.siege")
                .formatted(Formatting.BOLD, Formatting.GOLD, Formatting.UNDERLINE));
        this.config = this.game.config;
    }

    private static int getSortIndex(SiegeFlag flag, long time) {
        if (flag.isFrontLine(time) || flag.captureProgressTicks > 0) {
            return SORT_THREATENED;
        } else if (flag.team == SiegeTeams.ATTACKERS) {
            return SORT_ATTACKERS;
        } else if (flag.team == SiegeTeams.DEFENDERS) {
            return SORT_DEFENDERS;
        }
        return -1;
    }

    public void update(long time) {
        this.widget.set(content -> {
            if (this.config.capturingGiveTimeSecs() > 0) {
                content.add(
                        Text.translatable("game.siege.quick.sidebar").formatted(Formatting.GOLD),
                        Text.literal(this.config.giveTimeFormatted()).formatted(Formatting.AQUA)
                );
                content.add(ScreenTexts.EMPTY);
            }

            List<SiegeFlag> flags = new ArrayList<>(this.game.map.flags);
            flags.sort(Comparator.comparingInt(flag -> getSortIndex(flag, time)));

            long seconds = time / 20;
            boolean blink = seconds % 2 == 0;

            for (SiegeFlag flag : flags) {
                if (!flag.capturable) {
                    continue;
                }

                boolean italic = false;
                Formatting color = flag.team.config().chatFormatting();

                var blinkType = flag.capturingState != null ? flag.capturingState.getBlink() : NO_BLINK;
                switch (blinkType) {
                    case NO_BLINK -> {
                    }
                    case OWNING_TEAM_TO_CAPTURING -> {
                        GameTeam blinkTeam = blink ? SiegeTeams.ATTACKERS : SiegeTeams.DEFENDERS;
                        color = blinkTeam.config().chatFormatting();
                        italic = blinkTeam != flag.team;
                    }
                    case OWNING_TEAM_TO_GREY -> color = blink ? color : Formatting.GRAY;
                }

                var flagName = Text.literal(flag.name).formatted(color);

                if (italic) {
                    flagName.formatted(Formatting.ITALIC);
                }

                int percent = (int) Math.floor(flag.captureFraction() * 100);

                Text line;
                boolean underAttack = flag.capturingState != null && flag.isFlagUnderAttack();
                if (underAttack || percent > 0) {
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
}
