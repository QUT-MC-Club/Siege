package io.github.restioson.siege.game;

import io.github.restioson.siege.game.map.SiegeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.GameCloseListener;
import xyz.nucleoid.plasmid.game.event.GameOpenListener;
import xyz.nucleoid.plasmid.game.event.GameTickListener;
import xyz.nucleoid.plasmid.game.event.OfferPlayerListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.PlayerRemoveListener;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.game.player.PlayerSet;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.util.PlayerRef;
import xyz.nucleoid.plasmid.widget.GlobalWidgets;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SiegeActive {
    private final SiegeConfig config;

    public final GameSpace gameSpace;
    private final SiegeMap map;

    private final Object2ObjectMap<PlayerRef, SiegePlayer> participants;
    private final SiegeStageManager idle;
    private final SiegeTimerBar timerBar;

    private SiegeActive(GameSpace gameSpace, SiegeMap map, SiegeConfig config, Set<PlayerRef> participants, GlobalWidgets widgets) {
        this.gameSpace = gameSpace;
        this.config = config;
        this.map = map;
        this.participants = new Object2ObjectOpenHashMap<>();

        for (PlayerRef player : participants) {
            this.participants.put(player, new SiegePlayer(SiegeTeam.ATTACKERS));
        }

        this.idle = new SiegeStageManager();
        this.timerBar = new SiegeTimerBar(widgets);
    }

    public static void open(GameSpace gameSpace, SiegeMap map, SiegeConfig config) {
        gameSpace.openGame(game -> {
            GlobalWidgets widgets = new GlobalWidgets(game);

            Set<PlayerRef> participants = gameSpace.getPlayers().stream()
                    .map(PlayerRef::of)
                    .collect(Collectors.toSet());
            SiegeActive active = new SiegeActive(gameSpace, map, config, participants, widgets);

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
        for(PlayerRef ref : this.participants.keySet()) {
            ref.ifOnline(world, this::spawnParticipant);
        }

        this.idle.onOpen(world.getTime(), this.config);
    }

    private void onClose() {}

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

        SiegeStageManager.TickResult result = this.idle.tick(time, this.gameSpace);

        switch (result) {
            case CONTINUE_TICK: break;
            case TICK_FINISHED: return;
            case GAME_FINISHED:
                this.broadcastWin();
                return;
            case GAME_CLOSED:
                this.gameSpace.close(GameCloseReason.FINISHED);
                return;
        }

        if (time % 20 == 0) {
            this.tickCaptureFlags(world, 20);
        }

        this.timerBar.update(this.idle.finishTime - time, this.config.timeLimitMins * 20 * 60);
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
                    SiegeTeam team = siegePlayer.team;
                    if (team == SiegeTeam.DEFENDERS) {
                        defendersPresent++;
                    } else if (team == SiegeTeam.ATTACKERS) {
                        attackersPresent++;
                    }
                }
            }

            boolean defendersCapturing = defendersPresent > 0;
            boolean attackersCapturing = attackersPresent > 0;
            boolean contested = defendersCapturing && attackersCapturing;

            if (!contested && (defendersCapturing || attackersCapturing)) {
                SiegeTeam captureTeam;
                int captureCount;
                if (defendersCapturing) {
                    captureTeam = SiegeTeam.DEFENDERS;
                    captureCount = defendersPresent;
                } else {
                    captureTeam = SiegeTeam.ATTACKERS;
                    captureCount = attackersPresent;
                }

                // Cannot capture own flag
                if (captureTeam == flag.team) {
                    continue;
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

    private void broadcastCapture(SiegeFlag flag, SiegeTeam captureTeam) {
        this.gameSpace.getPlayers().sendMessage(
                new LiteralText("The ")
                        .append(new LiteralText(flag.name).formatted(Formatting.YELLOW))
                        .append(" has been captured by the ")
                        .append(captureTeam.getName())
                        .append("!")
        );
    }

    private void broadcastWin() {
        SiegeTeam winningTeam = SiegeTeam.ATTACKERS; // TODO
        Text message = new LiteralText("The ").append(winningTeam.getName()).append(" have won the game!").formatted(Formatting.GOLD);

        PlayerSet players = this.gameSpace.getPlayers();
        players.sendMessage(message);
        players.sendSound(SoundEvents.ENTITY_VILLAGER_YES);
    }
}
