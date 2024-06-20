package io.github.restioson.siege.game.active;

import io.github.restioson.siege.entity.SiegeKitStandEntity;
import io.github.restioson.siege.game.SiegeSpawnLogic;
import io.github.restioson.siege.game.SiegeTeams;
import io.github.restioson.siege.game.active.capturing.CapturingState;
import io.github.restioson.siege.game.map.SiegeFlag;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.util.PlayerRef;
import xyz.nucleoid.plasmid.util.Scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class SiegeCaptureLogic {
    public static final int CAPTURE_TIME_TICKS = 20 * 40;

    private final ServerWorld world;
    private final GameSpace gameSpace;
    private final SiegeActive game;

    private final List<ServerPlayerEntity> defendersPresent = new ArrayList<>();
    private final List<ServerPlayerEntity> attackersPresent = new ArrayList<>();
    private final Set<ServerPlayerEntity> playersPresent = new ReferenceOpenHashSet<>();

    SiegeCaptureLogic(SiegeActive game) {
        this.world = game.world;
        this.gameSpace = game.gameSpace;
        this.game = game;
    }

    void tick(ServerWorld world, int interval) {
        for (SiegeFlag flag : this.game.map.flags) {
            if (flag.capturable) {
                this.tickCaptureFlag(world, flag, interval);
            }
        }
    }

    private void tickCaptureFlag(ServerWorld world, SiegeFlag flag, int interval) {
        List<ServerPlayerEntity> defendersPresent = this.defendersPresent;
        List<ServerPlayerEntity> attackersPresent = this.attackersPresent;
        Set<ServerPlayerEntity> playersPresent = this.playersPresent;

        defendersPresent.clear();
        attackersPresent.clear();
        playersPresent.clear();

        for (Object2ObjectMap.Entry<PlayerRef, SiegePlayer> entry : Object2ObjectMaps.fastIterable(this.game.participants)) {
            ServerPlayerEntity player = entry.getKey().getEntity(world);
            if (player == null) {
                continue;
            }

            if (player.interactionManager.getGameMode() != GameMode.SURVIVAL) {
                continue;
            }

            SiegePlayer participant = entry.getValue();

            if (flag.bounds.contains(player.getBlockPos())) {
                GameTeam team = participant.team;
                if (team == SiegeTeams.DEFENDERS) {
                    defendersPresent.add(player);
                } else if (team == SiegeTeams.ATTACKERS) {
                    attackersPresent.add(player);
                }
            }
        }

        playersPresent.addAll(attackersPresent);
        playersPresent.addAll(defendersPresent);

        boolean recapture = this.game.config.recapture();
        boolean attackersAtFlag = !attackersPresent.isEmpty();
        boolean defendersAtFlag = !defendersPresent.isEmpty();
        var prereqs = flag.getUnmetPrerequisites();
        CapturingState state;

        if (defendersAtFlag && flag.team != SiegeTeams.DEFENDERS && !recapture) {
            // Defenders _would_ capture/contest, but it's disabled
            state = CapturingState.recaptureDisabled();
        } else if (defendersAtFlag && attackersAtFlag) {
            // Both teams present - contested
            state = prereqs.isEmpty() ? CapturingState.contested() : CapturingState.prerequisitesRequired(prereqs);
        } else if ((attackersAtFlag && flag.team != SiegeTeams.ATTACKERS) ||
                (defendersAtFlag && flag.team != SiegeTeams.DEFENDERS)) {
            // Only one team present - capturing if prerequisites met
            state = prereqs.isEmpty() ? CapturingState.capturing() : CapturingState.prerequisitesRequired(prereqs);
        } else {
            // Only owner team (or no players) present
            state = flag.captureProgressTicks > 0 ? CapturingState.securing() : null;
        }

        flag.capturingState = state;

        if (state == CapturingState.capturing()) {
            var team = attackersAtFlag ? SiegeTeams.ATTACKERS : SiegeTeams.DEFENDERS;
            this.tickCapturing(flag, interval, team, playersPresent);
        } else if (state == CapturingState.securing()) {
            this.tickSecuring(flag, interval, playersPresent);
        } else if (state == CapturingState.contested()) {
            this.tickContested(flag);
        }

        flag.updateCaptureBar();
        flag.updateCapturingPlayers(playersPresent);
    }

    private void tickCapturing(SiegeFlag flag, int interval, GameTeam captureTeam,
                               Set<ServerPlayerEntity> capturingPlayers) {
        // Just began capturing
        if (flag.captureProgressTicks == 0) {
            this.broadcastStartCapture(flag, captureTeam);
        }

        if (flag.incrementCapture(captureTeam, interval * capturingPlayers.size())) {
            for (SiegeKitStandEntity kitStand : flag.kitStands) {
                kitStand.onControllingFlagCaptured();
            }

            for (ServerPlayerEntity player : capturingPlayers) {
                SiegePlayer participant = this.game.participant(player);
                if (participant != null) {
                    participant.captures += 1;
                }
            }

            this.broadcastCaptured(flag, captureTeam);
            flag.setTeamBlocks(this.game.world, captureTeam);
            this.game.addTime(this.game.config.capturingGiveTimeSecs());

            for (ServerPlayerEntity player : capturingPlayers) {
                player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.NEUTRAL, 1.0F, 1.0F);
            }

            Text sub = null;
            if (this.game.config.capturingGiveTimeSecs() > 0) {
                sub = Text.empty()
                        .append(SiegeTeams.ATTACKERS.config().name())
                        .append(ScreenTexts.SPACE)
                        .append(Text.translatable("game.siege.flag.captured.extra_time.1"))
                        .append(ScreenTexts.SPACE)
                        .append(Text.literal(this.game.config.giveTimeFormatted()).formatted(Formatting.AQUA))
                        .append(ScreenTexts.SPACE)
                        .append(Text.translatable("game.siege.flag.captured.extra_time.2"));
            }

            this.game.showTitle(
                    captureTeam,
                    Text.translatable("game.siege.flag.captured.won", flag.name).formatted(Formatting.GREEN),
                    sub
            );

            this.game.showTitle(
                    SiegeTeams.opposite(captureTeam),
                    Text.translatable("game.siege.flag.captured.lost", flag.name).formatted(Formatting.RED),
                    sub
            );
        } else {
            flag.playSound(this.world, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F + flag.captureFraction());
        }

        var particles = captureTeam == SiegeTeams.ATTACKERS ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME;
        flag.spawnParticles(this.world, particles);
    }

    private void tickContested(SiegeFlag flag) {
        flag.playSound(this.world, SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO.value(), 1.0F);
        flag.spawnParticles(this.world, ParticleTypes.ANGRY_VILLAGER);
    }

    private void tickSecuring(SiegeFlag flag, int interval, Set<ServerPlayerEntity> securingPlayers) {
        if (flag.decrementCapture(interval * (securingPlayers.size() + 1))) {
            this.broadcastSecured(flag);

            for (ServerPlayerEntity player : securingPlayers) {
                SiegePlayer participant = this.game.participant(player);
                if (participant != null) {
                    participant.secures += 1;
                }
            }
        }

        flag.playSound(this.world, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 1.0F + flag.captureFraction());
        flag.spawnParticles(this.world, ParticleTypes.COMPOSTER);
    }

    private void broadcastStartCapture(SiegeFlag flag, GameTeam captureTeam) {
        var capture = captureTeam == SiegeTeams.ATTACKERS ? "captured" : "recaptured";

        this.gameSpace.getPlayers().sendMessage(
                Text.literal("The ")
                        .append(Text.literal(flag.name).formatted(Formatting.YELLOW))
                        .append(ScreenTexts.SPACE)
                        .append(flag.presentTobe())
                        .append(" being ")
                        .append(capture)
                        .append(" by the ")
                        .append(captureTeam.config().name())
                        .append("...")
                        .formatted(Formatting.BOLD)
        );

        this.gameSpace.getPlayers().playSound(SoundEvents.BLOCK_BELL_USE);

        for (Object2ObjectMap.Entry<PlayerRef, SiegePlayer> entry : Object2ObjectMaps.fastIterable(this.game.participants)) {
            if (entry.getValue().team == captureTeam) {
                continue;
            }

            entry.getKey().ifOnline(
                    this.world,
                    player -> {
                        AtomicInteger plays = new AtomicInteger();
                        Scheduler.INSTANCE.repeatWhile(
                                s -> player.playSound(SoundEvents.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1.0f, 1.0f),
                                t -> plays.incrementAndGet() < 3,
                                0,
                                7
                        );
                    }
            );
        }
    }

    private void broadcastCaptured(SiegeFlag flag, GameTeam captureTeam) {
        this.gameSpace.getPlayers().sendMessage(
                Text.literal("The ")
                        .append(Text.literal(flag.name).formatted(Formatting.YELLOW))
                        .append(ScreenTexts.SPACE)
                        .append(flag.pastToBe())
                        .append(" been captured by the ")
                        .append(captureTeam.config().name())
                        .append("!")
                        .formatted(Formatting.BOLD)
        );

        Vec3d pos = SiegeSpawnLogic.choosePos(this.world.getRandom(), flag.bounds, 0.0f);
        LightningEntity lightningEntity = EntityType.LIGHTNING_BOLT.create(this.world);
        Objects.requireNonNull(lightningEntity).refreshPositionAfterTeleport(pos);
        lightningEntity.setCosmetic(true);
        this.world.spawnEntity(lightningEntity);
    }

    private void broadcastSecured(SiegeFlag flag) {
        this.gameSpace.getPlayers().sendMessage(
                Text.literal("The ")
                        .append(Text.literal(flag.name).formatted(Formatting.YELLOW))
                        .append(ScreenTexts.SPACE)
                        .append(flag.pastToBe())
                        .append(" been defended by the ")
                        .append(flag.team.config().name())
                        .append("!")
                        .formatted(Formatting.BOLD)
        );
    }
}
