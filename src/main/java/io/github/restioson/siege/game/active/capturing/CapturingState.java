package io.github.restioson.siege.game.active.capturing;

import io.github.restioson.siege.game.map.SiegeFlag;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeam;

import java.util.List;

public interface CapturingState {
    static CapturingState capturing() {
        return SimpleCapturingState.CAPTURING;
    }

    static CapturingState contested() {
        return SimpleCapturingState.CONTESTED;
    }

    static CapturingState securing() {
        return SimpleCapturingState.SECURING;
    }

    static CapturingState recaptureDisabled() {
        return SimpleCapturingState.RECAPTURE_DISABLED;
    }

    static CapturingState prerequisitesRequired(List<SiegeFlag> prerequisites) {
        return new PrerequisitesRequired(prerequisites);
    }

    Text getTitle();

    boolean isUnderAttack();

    @NotNull
    BossBar.Color getCaptureBarColorForTeam(GameTeam team);

    @NotNull
    CapturingStateSidebarBlink getBlink();
}
