package io.github.restioson.siege.game.active;

import com.google.common.collect.Multimap;
import io.github.restioson.siege.entity.SiegeKitStandEntity;
import io.github.restioson.siege.game.SiegeConfig;
import io.github.restioson.siege.game.SiegeKit;
import io.github.restioson.siege.game.SiegeSpawnLogic;
import io.github.restioson.siege.game.SiegeTeams;
import io.github.restioson.siege.game.map.SiegeFlag;
import io.github.restioson.siege.game.map.SiegeKitStandLocation;
import io.github.restioson.siege.game.map.SiegeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
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

import java.util.Map;

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
    final SiegeGateLogic gateLogic;

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
        this.gateLogic = new SiegeGateLogic(this);
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
            game.setRule(GameRule.PLACE_BLOCKS, RuleResult.ALLOW);

            game.on(GameOpenListener.EVENT, active::onOpen);
            game.on(GameCloseListener.EVENT, active::onClose);
            game.on(DropItemListener.EVENT, active::onDropItem);
            game.on(BreakBlockListener.EVENT, active::onBreakBlock);
            game.on(PlaceBlockListener.EVENT, active::onPlaceBlock);

            game.on(OfferPlayerListener.EVENT, player -> JoinResult.ok());
            game.on(PlayerAddListener.EVENT, active::addPlayer);
            game.on(PlayerRemoveListener.EVENT, active::removePlayer);
            game.on(UseBlockListener.EVENT, active::onUseBlock);
            game.on(PlayerPunchBlockListener.EVENT, active::onHitBlock);

            game.on(GameTickListener.EVENT, active::tick);

            game.on(PlayerDamageListener.EVENT, active::onPlayerDamage);
            game.on(PlayerDeathListener.EVENT, active::onPlayerDeath);
            game.on(PlayerFireArrowListener.EVENT, active::onPlayerFireArrow);
            game.on(AttackEntityListener.EVENT, active::onAttackEntity);
            game.on(EntityHitListener.EVENT, active::onHitEntity);

            ServerWorld world = gameSpace.getWorld();

            for (SiegeKitStandLocation stand : active.map.kitStands) {
                SiegeKitStandEntity standEntity = new SiegeKitStandEntity(world, active, stand);
                world.spawnEntity(standEntity);
            }
        });
    }

    private ActionResult onHitEntity(ProjectileEntity projectileEntity, EntityHitResult hitResult) {
        if (hitResult.getEntity() instanceof ServerPlayerEntity) {
            return ActionResult.PASS;
        } else {
            return ActionResult.FAIL;
        }
    }

    private ActionResult onAttackEntity(ServerPlayerEntity playerEntity, Hand hand, Entity entity, EntityHitResult entityHitResult) {
        if (entity instanceof ServerPlayerEntity) {
            return ActionResult.PASS;
        } else {
            return ActionResult.FAIL;
        }
    }

    private ActionResult onHitBlock(ServerPlayerEntity player, Direction direction, BlockPos pos) {
        SiegePlayer participant = this.participant(player);
        if (participant != null) {
            return this.gateLogic.maybeBash(pos, player, participant);
        } else {
            return ActionResult.PASS;
        }
    }

    @Nullable
    public SiegePlayer participant(ServerPlayerEntity player) {
        return this.participant(PlayerRef.of(player));
    }

    @Nullable
    public SiegePlayer participant(PlayerRef player) {
        return this.participants.get(player);
    }

    private void onOpen() {
        ServerWorld world = this.gameSpace.getWorld();
        for (Map.Entry<PlayerRef, SiegePlayer> entry : this.participants.entrySet()) {
            entry.getKey().ifOnline(world, p -> this.spawnParticipant(p, this.map.getFirstSpawn(entry.getValue().team)));
        }

        this.stageManager.onOpen(world.getTime(), this.config);
    }

    private void onClose() {
    }

    private void addPlayer(ServerPlayerEntity player) {
        if (!this.participants.containsKey(PlayerRef.of(player))) {
            this.allocateParticipant(player);
        }
        this.spawnParticipant(player, null);
    }

    private void allocateParticipant(ServerPlayerEntity player) {
        GameTeam smallestTeam = this.getSmallestTeam();
        SiegePlayer participant = new SiegePlayer(smallestTeam);
        this.participants.put(PlayerRef.of(player), participant);
    }

    private GameTeam getSmallestTeam() {
        // TODO: store a map of teams to players, this is bad
        int attackersCount = 0;
        int defendersCount = 0;
        for (SiegePlayer participant : this.participants.values()) {
            if (participant.team == SiegeTeams.DEFENDERS) defendersCount++;
            if (participant.team == SiegeTeams.ATTACKERS) attackersCount++;
        }

        if (attackersCount < defendersCount) {
            return SiegeTeams.ATTACKERS;
        } else {
            return SiegeTeams.DEFENDERS;
        }
    }

    private void removePlayer(ServerPlayerEntity player) {
        SiegePlayer participant = this.participants.remove(PlayerRef.of(player));
        if (participant != null) {
            this.teams.removePlayer(player, participant.team);
        }
    }

    private ActionResult onDropItem(PlayerEntity player, int slot, ItemStack stack) {
        return ActionResult.FAIL;
    }

    private ActionResult onPlaceBlock(
            ServerPlayerEntity player,
            BlockPos blockPos,
            BlockState blockState,
            ItemUsageContext ctx
    ) {
        SiegePlayer participant = this.participant(player);
        if (participant != null && participant.kit != SiegeKit.CONSTRUCTOR) {
            return ActionResult.FAIL;
        }

        int slot;
        if (ctx.getHand() == Hand.MAIN_HAND) {
            slot = player.inventory.selectedSlot;
        } else {
            slot = 40; // offhand
        }

        for (BlockBounds noBuildRegion : this.map.noBuildRegions) {
            if (noBuildRegion.contains(blockPos)) {
                // TODO do this in plasmid
                player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(-2, slot, ctx.getStack()));
                return ActionResult.FAIL;
            }
        }

        return this.gateLogic.maybeBraceGate(blockPos, player, slot, ctx);
    }

    private ActionResult onBreakBlock(ServerPlayerEntity player, BlockPos pos) {
        if (this.map.isProtectedBlock(pos.asLong())) {
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    private ActionResult onUseBlock(ServerPlayerEntity player, Hand hand, BlockHitResult hitResult) {
        BlockPos pos = hitResult.getBlockPos();
        if (pos == null) {
            return ActionResult.PASS;
        }

        SiegePlayer participant = this.participant(player);
        if (participant != null) {
            pos.offset(hitResult.getSide());
            BlockState state = this.gameSpace.getWorld().getBlockState(pos);
            if (state.getBlock() instanceof EnderChestBlock) {
                participant.kit.restock(player, participant, this.gameSpace.getWorld());
            } else if (state.getBlock() instanceof DoorBlock) {
                return ActionResult.PASS;
            }

            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    private ActionResult onPlayerDamage(ServerPlayerEntity player, DamageSource source, float v) {
        SiegePlayer participant = this.participant(player);

        if (participant != null && this.gameSpace.getWorld().getTime() < participant.timeOfSpawn + 5 * 20) {
            return ActionResult.FAIL;
        }

        if (participant != null && source.getAttacker() != null && source.getAttacker() instanceof ServerPlayerEntity) {
            long time = this.gameSpace.getWorld().getTime();
            PlayerRef attacker = PlayerRef.of((ServerPlayerEntity) source.getAttacker());
            participant.lastTimeWasAttacked = new AttackRecord(attacker, time);
        }

        return ActionResult.PASS;
    }

    private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        MutableText deathMessage = this.getDeathMessage(player, source);
        this.gameSpace.getPlayers().sendMessage(deathMessage.formatted(Formatting.GRAY));

        this.spawnDeadParticipant(player);

        return ActionResult.FAIL;
    }

    private MutableText getDeathMessage(ServerPlayerEntity player, DamageSource source) {
        SiegePlayer participant = this.participant(player);
        ServerWorld world = this.gameSpace.getWorld();
        long time = world.getTime();

        MutableText eliminationMessage = new LiteralText(" was killed by ");
        if (source.getAttacker() != null) {
            eliminationMessage.append(source.getAttacker().getDisplayName());
        } else if (participant != null && participant.attacker(time, world) != null) {
            eliminationMessage.append(participant.attacker(time, world).getDisplayName());
        } else if (source == DamageSource.DROWN) {
            eliminationMessage.append("forgetting to just keep swimming");
        } else {
            eliminationMessage = new LiteralText(" died");
        }

        return new LiteralText("").append(player.getDisplayName()).append(eliminationMessage);
    }

    private ActionResult onPlayerFireArrow(
            ServerPlayerEntity user,
            ItemStack tool,
            ArrowItem arrowItem,
            int remainingUseTicks,
            PersistentProjectileEntity projectile
    ) {
        projectile.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
        return ActionResult.PASS;
    }

    private void spawnDeadParticipant(ServerPlayerEntity player) {
        player.inventory.clear();
        player.getEnderChestInventory().clear();
        player.setGameMode(GameMode.SPECTATOR);

        SiegePlayer siegePlayer = this.participant(player);
        if (siegePlayer != null) {
            siegePlayer.timeOfDeath = this.gameSpace.getWorld().getTime();
        }
    }

    private void spawnParticipant(ServerPlayerEntity player, @Nullable BlockBounds spawnRegion) {
        player.inventory.clear();
        player.getEnderChestInventory().clear();
        SiegePlayer participant = this.participant(player);
        assert participant != null; // spawnParticipant should only be spawned on a participant
        participant.kit.equipPlayer(player, participant);
        participant.timeOfSpawn = this.gameSpace.getWorld().getTime();

        if (spawnRegion == null) {
            spawnRegion = this.getRespawnFor(player);
        }

        SiegeSpawnLogic.resetPlayer(player, GameMode.SURVIVAL);
        SiegeSpawnLogic.spawnPlayer(player, spawnRegion, this.gameSpace.getWorld());
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
        SiegePlayer participant = this.participant(player);
        return participant != null ? participant.team : null;
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
            this.gateLogic.tick();
            this.sidebar.update(time);

            if (time % (20 * 2) == 0) {
                this.tickResources();
            }
        }

        this.tickDead(world, time);
        this.timerBar.update(this.stageManager.finishTime - time, this.config.timeLimitMins * 20 * 60);
    }

    private void tickResources() {
        for (SiegePlayer player : this.participants.values()) {
            player.incrementResource(SiegePersonalResource.WOOD, 1);
        }
    }

    private void tickDead(ServerWorld world, long time) {
        for (Object2ObjectMap.Entry<PlayerRef, SiegePlayer> entry : Object2ObjectMaps.fastIterable(this.participants)) {
            PlayerRef ref = entry.getKey();
            SiegePlayer state = entry.getValue();
            ref.ifOnline(world, p -> {
                if (p.isSpectator()) {
                    int respawnDelay = 5;

                    if (state.team == SiegeTeams.DEFENDERS) {
                        respawnDelay = 10;
                    }

                    int sec = respawnDelay - (int) Math.floor((time - state.timeOfDeath) / 20.0f);

                    if (sec > 0 && (time - state.timeOfDeath) % 20 == 0) {
                        Text text = new LiteralText(String.format("Respawning in %ds", sec)).formatted(Formatting.BOLD);
                        p.sendMessage(text, true);
                    }

                    if (time - state.timeOfDeath > respawnDelay * 20) {
                        this.spawnParticipant(p, null);
                    }
                }
            });
        }
    }

    private void broadcastWin(GameTeam winningTeam) {
        for (Map.Entry<PlayerRef, SiegePlayer> entry : this.participants.entrySet()) {
            entry.getKey().ifOnline(this.gameSpace.getServer(), player -> {
                if (entry.getValue().team == winningTeam && winningTeam == SiegeTeams.ATTACKERS) {
                    player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.0F, 1.0F);
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, 10, 0, false, false, true));
                }

                if (winningTeam == SiegeTeams.ATTACKERS) {
                    player.playSound(SoundEvents.ENTITY_RAVAGER_CELEBRATE, SoundCategory.MASTER, 1.0F, 1.0F);
                    player.playSound(SoundEvents.ENTITY_PILLAGER_CELEBRATE, SoundCategory.MASTER, 1.0F, 1.0F);
                }

                if (winningTeam == SiegeTeams.DEFENDERS) {
                    player.playSound(SoundEvents.ENTITY_VILLAGER_CELEBRATE, SoundCategory.MASTER, 1.0F, 1.0F);
                }
            });
        }

        Text message = new LiteralText("The ")
                .append(winningTeam.getDisplay())
                .append(" have won the game!")
                .formatted(winningTeam.getFormatting(), Formatting.BOLD);

        PlayerSet players = this.gameSpace.getPlayers();
        players.sendMessage(message);
        players.sendSound(SoundEvents.ENTITY_VILLAGER_YES);
    }
}
