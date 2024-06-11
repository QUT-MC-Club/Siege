package io.github.restioson.siege.game.map;

import io.github.restioson.siege.entity.SiegeKitStandEntity;
import io.github.restioson.siege.game.SiegeTeams;
import io.github.restioson.siege.game.active.CapturingState;
import io.github.restioson.siege.game.active.SiegeCaptureLogic;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class SiegeFlag {
    public final String id;
    public final String name;
    public final BlockBounds bounds;
    public boolean capturable = true;
    public boolean pluralName = false;
    public List<BlockBounds> flagIndicatorBlocks;
    public List<SiegeKitStandEntity> kitStands;

    @Nullable
    public ItemStack icon;

    @Nullable
    public SiegeSpawn attackerRespawn;
    @Nullable
    public SiegeSpawn defenderRespawn;

    public List<SiegeGate> gates;

    public GameTeam team;

    public CapturingState capturingState;
    public int captureProgressTicks;

    // The flags which must be captured before this flag can be captured
    public List<SiegeFlag> prerequisiteFlags = new ArrayList<>();
    public List<SiegeFlag> recapturePrerequisites = new ArrayList<>();

    public final ServerBossBar captureBar = new ServerBossBar(Text.literal("Capturing"), BossBar.Color.RED, BossBar.Style.NOTCHED_10);
    private final Set<ServerPlayerEntity> capturingPlayers = new ReferenceOpenHashSet<>();

    public SiegeFlag(String id, String name, GameTeam team, BlockBounds bounds) {
        this.id = id;
        this.name = name;
        this.team = team;
        this.bounds = bounds;
        this.kitStands = new ArrayList<>();
    }

    @Nullable
    public SiegeSpawn getRespawnFor(GameTeam team) {
        if (team == SiegeTeams.ATTACKERS) {
            return this.attackerRespawn;
        } else {
            return this.defenderRespawn;
        }
    }

    public String pastToBe() {
        if (this.pluralName) {
            return "have";
        } else {
            return "has";
        }
    }

    public String presentTobe() {
        if (this.pluralName) {
            return "are";
        } else {
            return "is";
        }
    }

    public boolean isReadyForCapture() {
        if (this.team == SiegeTeams.ATTACKERS) {
            for (SiegeFlag flag : this.recapturePrerequisites) {
                if (flag.team == this.team) {
                    return false;
                }
            }
        }

        for (SiegeFlag flag : this.prerequisiteFlags) {
            if (flag.team == this.team) {
                return false;
            }
        }

        return true;
    }

    public boolean incrementCapture(GameTeam team, int amount) {
        this.captureProgressTicks += amount;

        if (this.captureProgressTicks >= SiegeCaptureLogic.CAPTURE_TIME_TICKS) {
            this.captureProgressTicks = 0;
            this.team = team;
            this.capturingState = null;

            return true;
        }

        return false;
    }

    public boolean decrementCapture(int amount) {
        this.captureProgressTicks -= amount;

        if (this.captureProgressTicks <= 0) {
            this.captureProgressTicks = 0;
            this.capturingState = null;

            return true;
        }

        return false;
    }

    public void updateCapturingPlayers(Collection<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            if (this.capturingPlayers.add(player)) {
                this.captureBar.addPlayer(player);
            }
        }

        this.capturingPlayers.removeIf(player -> {
            if (!players.contains(player)) {
                this.captureBar.removePlayer(player);
                return true;
            }
            return false;
        });
    }

    public void updateCaptureBar() {
        if (this.capturingState != null) {
            this.captureBar.setVisible(true);
            this.captureBar.setName(this.capturingState.getName());
            this.captureBar.setPercent((float) this.captureProgressTicks / SiegeCaptureLogic.CAPTURE_TIME_TICKS);

            BossBar.Color color;
            if (this.capturingState != CapturingState.CONTESTED) {
                color = this.team == SiegeTeams.ATTACKERS ? BossBar.Color.RED : BossBar.Color.BLUE;
            } else {
                color = BossBar.Color.WHITE;
            }

            this.captureBar.setColor(color);
        } else {
            this.captureBar.setVisible(false);
        }
    }

    public void closeCaptureBar() {
        this.captureBar.clearPlayers();
        this.captureBar.setVisible(false);
    }

    public boolean isFrontLine(long time) {
        CapturingState state = this.capturingState;
        return (state != null && state.hasAlert() && state != CapturingState.SECURING)
                || (this.gateUnderAttack(time));
    }

    public boolean gateUnderAttack(long time) {
        long lastBash = this.gates
                .stream()
                .map(gate -> gate.timeOfLastBash)
                .max(Long::compareTo)
                .stream()
                .findAny()
                .orElse(0L);

        return time - lastBash < 5 * 20;
    }
}
