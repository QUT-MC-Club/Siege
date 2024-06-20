package io.github.restioson.siege.game.active.capturing;

import io.github.restioson.siege.game.map.SiegeFlag;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;

import java.util.List;

class PrerequisitesRequired implements CapturingState {
    private final List<SiegeFlag> prerequisites;

    PrerequisitesRequired(List<SiegeFlag> prerequisites) {
        this.prerequisites = prerequisites;
    }

    @Override
    public Text getTitle() {
        var text = Text.translatable("game.siege.flag.prerequisite_required");
        text.append(" ");

        for (int i = 0; i < this.prerequisites.size(); i++) {
            text.append(this.prerequisites.get(i).name);

            if (this.prerequisites.size() > 1) {
                if (i != this.prerequisites.size() - 1) {
                    text.append(this.prerequisites.size() > 2 ? ", " : " ");
                }

                if (i == this.prerequisites.size() - 2) {
                    text.append(Text.translatable("game.siege.flag.prerequisite_required.and"));
                    text.append(" ");
                }
            }
        }

        return text;
    }

    @Override
    public boolean isUnderAttack() {
        return false;
    }

    @Override
    public @NotNull BossBar.Color getCaptureBarColorForTeam(GameTeam team) {
        return BossBar.Color.RED;
    }

    @Override
    public @NotNull CapturingStateSidebarBlink getBlink() {
        return CapturingStateSidebarBlink.NO_BLINK;
    }
}
