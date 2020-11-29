package io.github.restioson.siege.game;

import com.google.common.collect.Multimap;
import io.github.restioson.siege.game.map.SiegeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.*;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.game.player.PlayerSet;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.util.PlayerRef;
import xyz.nucleoid.plasmid.util.Scheduler;
import xyz.nucleoid.plasmid.widget.GlobalWidgets;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class SiegeActive {
    private final SiegeConfig config;

    public final GameSpace gameSpace;
    final SiegeMap map;

    private final Object2ObjectMap<PlayerRef, SiegePlayer> participants;
    private final SiegeStageManager stageManager;

    private final SiegeSidebar sidebar;
    private final SiegeTimerBar timerBar;

    private SiegeActive(GameSpace gameSpace, SiegeMap map, SiegeConfig config, GlobalWidgets widgets, Multimap<GameTeam, ServerPlayerEntity> players) {
        this.gameSpace = gameSpace;
        this.config = config;
        this.map = map;
        this.participants = new Object2ObjectOpenHashMap<>();

        for (GameTeam team : players.keySet()) {
            for (ServerPlayerEntity player : players.get(team)) {
                this.participants.put(PlayerRef.of(player), new SiegePlayer(team));
            }
        }

        this.stageManager = new SiegeStageManager();

        this.sidebar = new SiegeSidebar(this, widgets);
        this.timerBar = new SiegeTimerBar(widgets);
    }

    public static void open(GameSpace gameSpace, SiegeMap map, SiegeConfig config, Multimap<GameTeam, ServerPlayerEntity> players) {
        gameSpace.openGame(game -> {
            GlobalWidgets widgets = new GlobalWidgets(game);

            SiegeActive active = new SiegeActive(gameSpace, map, config, widgets, players);

            game.setRule(GameRule.CRAFTING, RuleResult.DENY);
            game.setRule(GameRule.PORTALS, RuleResult.DENY);
            game.setRule(GameRule.PVP, RuleResult.ALLOW);
            game.setRule(GameRule.HUNGER, RuleResult.ALLOW);
            game.setRule(GameRule.INTERACTION, RuleResult.ALLOW);
            game.setRule(GameRule.FALL_DAMAGE, RuleResult.ALLOW);

            game.on(GameOpenListener.EVENT, active::onOpen);
            game.on(GameCloseListener.EVENT, active::onClose);

            game.on(OfferPlayerListener.EVENT, player -> JoinResult.ok());
            game.on(PlayerAddListener.EVENT, active::addPlayer);
            game.on(PlayerRemoveListener.EVENT, active::removePlayer);

            game.on(GameTickListener.EVENT, active::tick);

            game.on(PlayerDeathListener.EVENT, active::onPlayerDeath);
        });
    }

    private void onOpen() {
        ServerWorld world = this.gameSpace.getWorld();
        for (PlayerRef ref : this.participants.keySet()) {
            ref.ifOnline(world, this::spawnParticipant);
        }

        this.stageManager.onOpen(world.getTime(), this.config);
    }

    private void onClose() {
    }

    private void addPlayer(ServerPlayerEntity player) {
        if (!this.participants.containsKey(PlayerRef.of(player))) {
            this.spawnSpectator(player);
        }
    }

    private void removePlayer(ServerPlayerEntity player) {
        this.participants.remove(PlayerRef.of(player));
    }

    private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        this.spawnParticipant(player);
        return ActionResult.FAIL;
    }

    private void spawnParticipant(ServerPlayerEntity player) {
        player.inventory.clear();
        player.getEnderChestInventory().clear();

        SiegeSpawnLogic.resetPlayer(player, GameMode.ADVENTURE);
        SiegeSpawnLogic.spawnPlayer(player, this.map.waitingSpawn, this.gameSpace.getWorld()); // TODO change spawn
    }

    private void spawnSpectator(ServerPlayerEntity player) {
        SiegeSpawnLogic.resetPlayer(player, GameMode.SPECTATOR);
        SiegeSpawnLogic.spawnPlayer(player, this.map.waitingSpawn, this.gameSpace.getWorld());
    }

    private void tick() {
        ServerWorld world = this.gameSpace.getWorld();
        long time = world.getTime();

        SiegeStageManager.TickResult result = this.stageManager.tick(time, this.gameSpace);
        if (result != SiegeStageManager.TickResult.CONTINUE_TICK) {
            switch (result) {
                case GAME_FINISHED: this.broadcastWin();
                case GAME_CLOSED: this.gameSpace.close(GameCloseReason.FINISHED);
            }
            return;
        }

        if (time % 20 == 0) {
            this.tickCaptureFlags(world, 20);

            this.sidebar.update(time);
        }

        this.timerBar.update(this.stageManager.finishTime - time, this.config.timeLimitMins * 20 * 60);
    }

    private void tickCaptureFlags(ServerWorld world, int interval) {
        for (SiegeFlag flag : this.map.flags) {
            int defendersPresent = 0;
            int attackersPresent = 0;

            for (Object2ObjectMap.Entry<PlayerRef, SiegePlayer> entry : Object2ObjectMaps.fastIterable(this.participants)) {
                ServerPlayerEntity player = entry.getKey().getEntity(world);
                if (player == null) {
                    continue;
                }

                SiegePlayer siegePlayer = entry.getValue();

                if (flag.bounds.contains(player.getBlockPos())) {
                    GameTeam team = siegePlayer.team;
                    if (team == SiegeTeams.DEFENDERS) {
                        defendersPresent++;
                    } else if (team == SiegeTeams.ATTACKERS) {
                        attackersPresent++;
                    }
                }
            }

            boolean defendersCapturing = defendersPresent > 0;
            boolean attackersCapturing = attackersPresent > 0;
            boolean contested = defendersCapturing && attackersCapturing;
            boolean capturing = defendersCapturing || attackersCapturing;

            if (capturing && !contested) {
                GameTeam captureTeam;
                int captureCount;
                if (defendersCapturing) {
                    captureTeam = SiegeTeams.DEFENDERS;
                    captureCount = defendersPresent;
                } else {
                    captureTeam = SiegeTeams.ATTACKERS;
                    captureCount = attackersPresent;
                }

                // Cannot capture own flag
                if (captureTeam == flag.team) {
                    continue;
                }

                // Just began capturing
                if (flag.captureProgressTicks == 0) {
                    this.broadcastStartCapture(flag, captureTeam);
                }

                flag.captureProgressTicks += interval * captureCount;

                if (flag.captureProgressTicks >= 2 * 20) { // TODO: change to a more sensible value...
                    flag.captureProgressTicks = 0;
                    flag.team = captureTeam;

                    this.broadcastCapture(flag, captureTeam);
                }
            }
        }
    }

    private void broadcastStartCapture(SiegeFlag flag, GameTeam captureTeam) {
        this.gameSpace.getPlayers().sendMessage(
                new LiteralText("The ")
                        .append(new LiteralText(flag.name).formatted(Formatting.YELLOW))
                        .append(" is being captured by the ")
                        .append(new LiteralText(captureTeam.getDisplay()).formatted(captureTeam.getFormatting()))
                        .append("...")
                        .formatted(Formatting.BOLD)
        );

        this.gameSpace.getPlayers().sendSound(SoundEvents.BLOCK_BELL_USE);

        for (Object2ObjectMap.Entry<PlayerRef, SiegePlayer> entry : Object2ObjectMaps.fastIterable(this.participants)) {
            if (entry.getValue().team != captureTeam) {
                entry.getKey().ifOnline(
                        this.gameSpace.getWorld(),
                        p -> {
                            AtomicInteger plays = new AtomicInteger();
                            Scheduler.INSTANCE.repeatWhile(
                                    s -> p.playSound(SoundEvents.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1.0f, 1.0f),
                                    t -> plays.incrementAndGet() < 3,
                                    0,
                                    7
                            );
                        }
                );
            }
        }
    }

    private void broadcastCapture(SiegeFlag flag, GameTeam captureTeam) {
        this.gameSpace.getPlayers().sendMessage(
                new LiteralText("The ")
                        .append(new LiteralText(flag.name).formatted(Formatting.YELLOW))
                        .append(" has been captured by the ")
                        .append(new LiteralText(captureTeam.getDisplay()).formatted(captureTeam.getFormatting()))
                        .append("!")
                        .formatted(Formatting.BOLD)
        );

        ServerWorld world = this.gameSpace.getWorld();

        Vec3d pos = SiegeSpawnLogic.choosePos(world.getRandom(), flag.bounds);
        LightningEntity lightningEntity = EntityType.LIGHTNING_BOLT.create(world);
        Objects.requireNonNull(lightningEntity).refreshPositionAfterTeleport(pos);
        lightningEntity.setCosmetic(true);
        world.spawnEntity(lightningEntity);
    }

    private void broadcastWin() {
        GameTeam winningTeam = SiegeTeams.ATTACKERS; // TODO
        Text message = new LiteralText("The ")
                .append(winningTeam.getDisplay())
                .append(" have won the game!")
                .formatted(winningTeam.getFormatting(), Formatting.BOLD);

        PlayerSet players = this.gameSpace.getPlayers();
        players.sendMessage(message);
        players.sendSound(SoundEvents.ENTITY_VILLAGER_YES);
    }
}
