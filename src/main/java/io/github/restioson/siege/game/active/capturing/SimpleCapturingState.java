package io.github.restioson.siege.game.active.capturing;

import io.github.restioson.siege.game.SiegeTeams;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;

import static io.github.restioson.siege.game.active.capturing.CapturingStateSidebarBlink.*;

enum SimpleCapturingState implements CapturingState {
    CAPTURING(Text.literal("Capturing..").formatted(Formatting.GOLD), true, OWNING_TEAM_TO_CAPTURING),
    CONTESTED(Text.literal("Contested!").formatted(Formatting.GRAY), true, OWNING_TEAM_TO_GREY),
    SECURING(Text.literal("Securing..").formatted(Formatting.AQUA), false, OWNING_TEAM_TO_CAPTURING),
    RECAPTURE_DISABLED(Text.literal("Recapture is disabled for this game!").formatted(Formatting.RED), false, NO_BLINK);

    private final Text name;
    private final boolean isUnderAttack;
    private final CapturingStateSidebarBlink blink;

    SimpleCapturingState(Text name, boolean isUnderAttack, CapturingStateSidebarBlink blink) {
        this.name = name;
        this.isUnderAttack = isUnderAttack;
        this.blink = blink;
    }

    public Text getTitle() {
        return this.name;
    }

    @Override
    public boolean isUnderAttack() {
        return this.isUnderAttack;
    }

    @Override
    public @NotNull BossBar.Color getCaptureBarColorForTeam(GameTeam flagOwner) {
        return switch (this) {
            case CAPTURING -> flagOwner == SiegeTeams.DEFENDERS ? BossBar.Color.RED : BossBar.Color.BLUE;
            case CONTESTED -> BossBar.Color.WHITE;
            case SECURING -> BossBar.Color.GREEN;
            case RECAPTURE_DISABLED -> BossBar.Color.YELLOW;
        };
    }

    @Override
    public @NotNull CapturingStateSidebarBlink getBlink() {
        return this.blink;
    }
}
