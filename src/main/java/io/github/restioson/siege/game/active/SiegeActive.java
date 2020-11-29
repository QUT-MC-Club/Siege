package io.github.restioson.siege.game.active;

import com.google.common.collect.Multimap;
import io.github.restioson.siege.entity.SiegeKitStandEntity;
import io.github.restioson.siege.game.SiegeConfig;
import io.github.restioson.siege.game.SiegeSpawnLogic;
import io.github.restioson.siege.game.SiegeTeams;
import io.github.restioson.siege.game.map.SiegeFlag;
import io.github.restioson.siege.game.map.SiegeKitStandLocation;
import io.github.restioson.siege.game.map.SiegeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.*;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.game.player.PlayerSet;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.util.BlockBounds;
import xyz.nucleoid.plasmid.util.PlayerRef;
import xyz.nucleoid.plasmid.widget.GlobalWidgets;

public class SiegeActive {
    final SiegeConfig config;

    public final GameSpace gameSpace;
    final SiegeMap map;

    final SiegeTeams teams;

    public final Object2ObjectMap<PlayerRef, SiegePlayer> participants;
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
            game.on(DropItemListener.EVENT, active::onDropItem);
            game.on(BreakBlockListener.EVENT, active::onBreakBlock);

            game.on(OfferPlayerListener.EVENT, player -> JoinResult.ok());
            game.on(PlayerAddListener.EVENT, active::addPlayer);
            game.on(PlayerRemoveListener.EVENT, active::removePlayer);

            game.on(GameTickListener.EVENT, active::tick);

            game.on(PlayerDamageListener.EVENT, active::onPlayerDamage);
            game.on(PlayerDeathListener.EVENT, active::onPlayerDeath);

            ServerWorld world = gameSpace.getWorld();

            for (SiegeKitStandLocation stand : active.map.kitStands) {
                SiegeKitStandEntity standEntity = new SiegeKitStandEntity(world, active, stand);
                world.spawnEntity(standEntity);
            }
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

    private ActionResult onDropItem(PlayerEntity player, int slot, ItemStack stack) {
        if (stack.getItem() == Items.ARROW || stack.getItem() == Items.COOKED_BEEF) {
            return ActionResult.PASS;
        } else {
            return ActionResult.FAIL;
        }
    }

    private ActionResult onBreakBlock(ServerPlayerEntity player, BlockPos pos) {
        if (this.map.isProtectedBlock(pos.asLong())) {
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    private ActionResult onPlayerDamage(ServerPlayerEntity player, DamageSource source, float v) {
        SiegePlayer participant = this.participants.get(player);

        if (participant != null && source.getAttacker() != null && source.getAttacker() instanceof ServerPlayerEntity) {
            long time = this.gameSpace.getWorld().getTime();
            PlayerRef attacker = PlayerRef.of((ServerPlayerEntity) source.getAttacker());
            participant.lastTimeWasAttacked = new AttackRecord(attacker, time);
        }

        return ActionResult.PASS;
    }

    private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        PlayerSet players = this.gameSpace.getPlayers();
        MutableText eliminationMessage = new LiteralText(" was killed by ");
        SiegePlayer participant = this.participants.get(PlayerRef.of(player));
        ServerWorld world = this.gameSpace.getWorld();
        long time = world.getTime();

        if (source.getAttacker() != null) {
            eliminationMessage.append(source.getAttacker().getDisplayName());
        } else if (participant != null && participant.attacker(time, world) != null) {
            eliminationMessage.append(participant.attacker(time, world).getDisplayName());
        } else if (source == DamageSource.DROWN) {
            eliminationMessage.append("forgetting to just keep swimming");
        } else {
            eliminationMessage = new LiteralText(" died");
        }

        players.sendMessage(new LiteralText("").append(player.getDisplayName()).append(eliminationMessage).formatted(Formatting.GRAY));

        this.spawnParticipant(player);
        return ActionResult.FAIL;
    }

    private void spawnParticipant(ServerPlayerEntity player) {
        player.inventory.clear();
        player.getEnderChestInventory().clear();
        SiegePlayer siegePlayer = this.participants.get(PlayerRef.of(player));
        siegePlayer.kit.equipPlayer(player, siegePlayer.team);

        BlockBounds respawn = this.getRespawnFor(player);

        SiegeSpawnLogic.resetPlayer(player, GameMode.SURVIVAL);
        SiegeSpawnLogic.spawnPlayer(player, respawn, this.gameSpace.getWorld());
    }

    private BlockBounds getRespawnFor(ServerPlayerEntity player) {
        GameTeam team = this.getTeamFor(player);
        if (team == null) {
            return this.map.waitingSpawn;
        }

        BlockBounds respawn = this.map.waitingSpawn;
        double minDistance = Double.MAX_VALUE;

        for (SiegeFlag flag : this.map.flags) {
            if (flag.respawn != null && flag.team == team) {
                Vec3d center = flag.respawn.getCenter();
                double distance = player.squaredDistanceTo(center);
                if (distance < minDistance) {
                    respawn = flag.respawn;
                    minDistance = distance;
                }
            }
        }

        return respawn;
    }

    @Nullable
    private GameTeam getTeamFor(ServerPlayerEntity player) {
        SiegePlayer participant = this.participants.get(PlayerRef.of(player));
        return participant != null ? participant.team : null;
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
