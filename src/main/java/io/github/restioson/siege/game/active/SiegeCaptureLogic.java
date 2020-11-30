package io.github.restioson.siege.game.active;

import com.google.common.collect.ImmutableList;
import io.github.restioson.siege.game.SiegeSpawnLogic;
import io.github.restioson.siege.game.SiegeTeams;
import io.github.restioson.siege.game.map.SiegeFlag;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.util.PlayerRef;
import xyz.nucleoid.plasmid.util.Scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class SiegeCaptureLogic {
    public static final int CAPTURE_TIME_TICKS = 20 * 10;

    private final GameSpace gameSpace;
    private final SiegeActive game;

    private final List<ServerPlayerEntity> defendersPresent = new ArrayList<>();
    private final List<ServerPlayerEntity> attackersPresent = new ArrayList<>();

    SiegeCaptureLogic(SiegeActive game) {
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

        defendersPresent.clear();
        attackersPresent.clear();

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

        boolean defendersCapturing = !defendersPresent.isEmpty();
        boolean attackersCapturing = !attackersPresent.isEmpty();
        boolean contested = defendersCapturing && attackersCapturing;
        boolean capturing = defendersCapturing || attackersCapturing;

        CapturingState capturingState = null;
        GameTeam captureTeam = flag.team;
        List<ServerPlayerEntity> capturingPlayers = ImmutableList.of();

        if (capturing) {
            if (!contested) {
                if (defendersCapturing) {
                    captureTeam = SiegeTeams.DEFENDERS;
                    capturingPlayers = defendersPresent;
                } else {
                    captureTeam = SiegeTeams.ATTACKERS;
                    capturingPlayers = attackersPresent;
                }

                capturingState = captureTeam != flag.team ? CapturingState.CAPTURING : null;
            } else {
                capturingState = CapturingState.CONTESTED;
            }
        } else {
            if (flag.captureProgressTicks > 0) {
                capturingState = CapturingState.SECURING;
            }
        }

        flag.capturingState = capturingState;

        if (capturingState == CapturingState.CAPTURING) {
            this.tickCapturing(flag, interval, captureTeam, capturingPlayers);
        } else if (capturingState == CapturingState.SECURING) {
            this.tickSecuring(flag, interval);
        }
    }

    private void tickCapturing(SiegeFlag flag, int interval, GameTeam captureTeam, List<ServerPlayerEntity> capturingPlayers) {
        // Just began capturing
        if (flag.captureProgressTicks == 0) {
            this.broadcastStartCapture(flag, captureTeam);
        }

        if (flag.incrementCapture(captureTeam, interval * capturingPlayers.size())) {
            this.broadcastCaptured(flag, captureTeam);

            if (flag.flagIndicatorBlocks != null) {
                ServerWorld world = this.game.gameSpace.getWorld();
                for (BlockPos blockPos : flag.flagIndicatorBlocks) {
                    BlockState blockState = world.getBlockState(blockPos);
                    if (blockState.getBlock() == Blocks.BLUE_WOOL || blockState.getBlock() == Blocks.RED_WOOL) {
                        Block wool;

                        if (captureTeam == SiegeTeams.DEFENDERS) {
                            wool = Blocks.BLUE_WOOL;
                        } else {
                            wool = Blocks.RED_WOOL;
                        }

                        world.setBlockState(blockPos, wool.getDefaultState());
                    }
                }
            }

            for (ServerPlayerEntity player : capturingPlayers) {
                player.sendMessage(new LiteralText("Captured!").formatted(Formatting.AQUA), true);
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.NEUTRAL, 1.0F, 1.0F);
            }
        } else {
            int capturePercent = flag.captureProgressTicks * 100 / CAPTURE_TIME_TICKS;
            for (ServerPlayerEntity player : capturingPlayers) {
                player.sendMessage(new LiteralText("Capturing: " + capturePercent + "%").formatted(Formatting.RED), true);
                player.playSound(SoundEvents.BLOCK_STONE_PLACE,  SoundCategory.NEUTRAL, 1.0F, 1.0F);
            }
        }
    }

    private void tickSecuring(SiegeFlag flag, int interval) {
        if (flag.decrementCapture(interval)) {
            this.broadcastSecured(flag);
        }
    }

    private void broadcastStartCapture(SiegeFlag flag, GameTeam captureTeam) {
        this.gameSpace.getPlayers().sendMessage(
                new LiteralText("The ")
                        .append(new LiteralText(flag.name).formatted(Formatting.YELLOW))
                        .append(" ")
                        .append(flag.presentTobe())
                        .append(" being captured by the ")
                        .append(new LiteralText(captureTeam.getDisplay()).formatted(captureTeam.getFormatting()))
                        .append("...")
                        .formatted(Formatting.BOLD)
        );

        this.gameSpace.getPlayers().sendSound(SoundEvents.BLOCK_BELL_USE);

        for (Object2ObjectMap.Entry<PlayerRef, SiegePlayer> entry : Object2ObjectMaps.fastIterable(this.game.participants)) {
            if (entry.getValue().team == captureTeam) {
                continue;
            }

            entry.getKey().ifOnline(
                    this.gameSpace.getWorld(),
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
                new LiteralText("The ")
                        .append(new LiteralText(flag.name).formatted(Formatting.YELLOW))
                        .append(" ")
                        .append(flag.pastToBe())
                        .append(" been captured by the ")
                        .append(new LiteralText(captureTeam.getDisplay()).formatted(captureTeam.getFormatting()))
                        .append("!")
                        .formatted(Formatting.BOLD)
        );

        ServerWorld world = this.gameSpace.getWorld();

        Vec3d pos = SiegeSpawnLogic.choosePos(world.getRandom(), flag.bounds, 0.0f);
        LightningEntity lightningEntity = EntityType.LIGHTNING_BOLT.create(world);
        Objects.requireNonNull(lightningEntity).refreshPositionAfterTeleport(pos);
        lightningEntity.setCosmetic(true);
        world.spawnEntity(lightningEntity);
    }

    private void broadcastSecured(SiegeFlag flag) {
        this.gameSpace.getPlayers().sendMessage(
                new LiteralText("The ")
                        .append(new LiteralText(flag.name).formatted(Formatting.YELLOW))
                        .append(" ")
                        .append(flag.pastToBe())
                        .append(" been defended by the ")
                        .append(new LiteralText(flag.team.getDisplay()).formatted(flag.team.getFormatting()))
                        .append("!")
                        .formatted(Formatting.BOLD)
        );
    }
}
