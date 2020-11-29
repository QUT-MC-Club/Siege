package io.github.restioson.siege.game;

import com.google.common.collect.Multimap;
import io.github.restioson.siege.game.map.SiegeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
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
import xyz.nucleoid.plasmid.game.event.*;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.game.player.PlayerSet;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.util.PlayerRef;
import xyz.nucleoid.plasmid.widget.GlobalWidgets;

public class SiegeActive {
    final SiegeConfig config;

    public final GameSpace gameSpace;
    final SiegeMap map;

    final SiegeTeams teams;

    final Object2ObjectMap<PlayerRef, SiegePlayer> participants;
    final SiegeStageManager stageManager;

    final SiegeSidebar sidebar;
    final SiegeTimerBar timerBar;

    final SiegeCaptureLogic captureLogic;

    private SiegeActive(GameSpace gameSpace, SiegeMap map, SiegeConfig config, GlobalWidgets widgets, Multimap<GameTeam, ServerPlayerEntity> players) {
        this.gameSpace = gameSpace;
        this.config = config;
        this.map = map;
        this.participants = new Object2ObjectOpenHashMap<>();

        this.teams = gameSpace.addResource(new SiegeTeams(gameSpace));

        for (GameTeam team : players.keySet()) {
            for (ServerPlayerEntity player : players.get(team)) {
                this.participants.put(PlayerRef.of(player), new SiegePlayer(team));
                this.teams.addPlayer(player, team);
            }
        }

        this.stageManager = new SiegeStageManager(this);

        this.sidebar = new SiegeSidebar(this, widgets);
        this.timerBar = new SiegeTimerBar(widgets);

        this.captureLogic = new SiegeCaptureLogic(this);
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

        SiegeStageManager.TickResult result = this.stageManager.tick(time);
        if (result != SiegeStageManager.TickResult.CONTINUE_TICK) {
            switch (result) {
                case ATTACKERS_WIN:
                    this.broadcastWin(SiegeTeams.ATTACKERS);
                    break;
                case DEFENDERS_WIN:
                    this.broadcastWin(SiegeTeams.DEFENDERS);
                    break;
                case GAME_CLOSED:
                    this.gameSpace.close(GameCloseReason.FINISHED);
                    break;
            }
            return;
        }

        if (time % 20 == 0) {
            this.captureLogic.tick(world, 20);

            this.sidebar.update(time);
        }

        this.timerBar.update(this.stageManager.finishTime - time, this.config.timeLimitMins * 20 * 60);
    }

    private void broadcastWin(GameTeam winningTeam) {
        Text message = new LiteralText("The ")
                .append(winningTeam.getDisplay())
                .append(" have won the game!")
                .formatted(winningTeam.getFormatting(), Formatting.BOLD);

        PlayerSet players = this.gameSpace.getPlayers();
        players.sendMessage(message);
        players.sendSound(SoundEvents.ENTITY_VILLAGER_YES);
    }
}
