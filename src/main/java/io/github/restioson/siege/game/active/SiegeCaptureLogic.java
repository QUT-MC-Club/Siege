package io.github.restioson.siege.game.active;

import com.google.common.collect.ImmutableList;
import io.github.restioson.siege.entity.SiegeKitStandEntity;
import io.github.restioson.siege.game.SiegeSpawnLogic;
import io.github.restioson.siege.game.SiegeTeams;
import io.github.restioson.siege.game.map.SiegeFlag;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
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
        boolean defendersCapturing = defendersAtFlag && recapture && flag.team != SiegeTeams.DEFENDERS;
        boolean attackersCapturing = attackersAtFlag && flag.team != SiegeTeams.ATTACKERS;
        boolean tryingToCapture = defendersCapturing || attackersCapturing;
        boolean contested = tryingToCapture && defendersAtFlag && attackersAtFlag;

        CapturingState capturingState = null;
        GameTeam captureTeam = flag.team;
        List<ServerPlayerEntity> capturingPlayers = ImmutableList.of();

        if (tryingToCapture) {
            if (!contested) {
                if (defendersAtFlag) {
                    captureTeam = SiegeTeams.DEFENDERS;
                    capturingPlayers = defendersPresent;
                } else {
                    captureTeam = SiegeTeams.ATTACKERS;
                    capturingPlayers = attackersPresent;
                }

                capturingState = CapturingState.CAPTURING;
            } else {
                capturingState = CapturingState.CONTESTED;
            }
        } else {
            if (flag.captureProgressTicks > 0) {
                capturingState = CapturingState.SECURING;
            }
        }

        if (capturingState != null) {
            if (defendersAtFlag && flag.team != SiegeTeams.DEFENDERS) {
                capturingState = CapturingState.RECAPTURE_DISABLED;
            } else if (!flag.isReadyForCapture()) {
                capturingState = CapturingState.PREREQUISITE_REQUIRED;
            }
        }

        flag.capturingState = capturingState;

        if (capturingState == CapturingState.CAPTURING) {
            this.tickCapturing(flag, interval, captureTeam, capturingPlayers);
        } else if (capturingState == CapturingState.SECURING) {
            this.tickSecuring(flag, interval, capturingPlayers);
        }

        flag.updateCaptureBar();
        flag.updateCapturingPlayers(playersPresent);
    }

    private void tickCapturing(SiegeFlag flag, int interval, GameTeam captureTeam, List<ServerPlayerEntity> capturingPlayers) {
        // Just began capturing
        if (flag.captureProgressTicks == 0) {
            this.broadcastStartCapture(flag, captureTeam);
        }

        int amount = capturingPlayers.stream()
                .map(p -> {
                    var participant = this.game.participant(p);
                    return participant != null ? participant.kit.captureProgressModifier() : 0;
                })
                .reduce(0, Integer::sum);

        if (flag.incrementCapture(captureTeam, interval * amount)) {
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

            for (ServerPlayerEntity player : capturingPlayers) {
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.NEUTRAL, 1.0F, 1.0F);
            }
        } else {
            for (ServerPlayerEntity player : capturingPlayers) {
                player.playSound(SoundEvents.BLOCK_STONE_PLACE,  SoundCategory.NEUTRAL, 1.0F, 1.0F);
            }
        }
    }

    private void tickSecuring(SiegeFlag flag, int interval, List<ServerPlayerEntity> securingPlayers) {
        if (flag.decrementCapture(interval)) {
            this.broadcastSecured(flag);

            for (ServerPlayerEntity player : securingPlayers) {
                SiegePlayer participant = this.game.participant(player);
                if (participant != null) {
                    participant.secures += 1;
                }
            }
        }
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
