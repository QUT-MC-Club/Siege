package io.github.restioson.siege.game.active;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import eu.pb4.sgui.api.gui.SimpleGui;
import io.github.restioson.siege.game.SiegeConfig;
import io.github.restioson.siege.game.SiegeKit;
import io.github.restioson.siege.game.SiegeSpawnLogic;
import io.github.restioson.siege.game.SiegeTeams;
import io.github.restioson.siege.game.map.*;
import io.github.restioson.siege.item.SiegeHorn;
import io.github.restioson.siege.item.SiegeItems;
import io.github.restioson.siege.mixin.ExplosionImplAccessor;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerPosition;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
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
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.PlayerLimiter;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.*;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.util.PlayerRef;
import xyz.nucleoid.stimuli.event.DroppedItemsResult;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.block.*;
import xyz.nucleoid.stimuli.event.entity.EntityDropItemsEvent;
import xyz.nucleoid.stimuli.event.item.ItemThrowEvent;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerAttackEntityEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;
import xyz.nucleoid.stimuli.event.projectile.ArrowFireEvent;
import xyz.nucleoid.stimuli.event.projectile.ProjectileHitEvent;
import xyz.nucleoid.stimuli.event.world.ExplosionDetonatedEvent;

import java.util.*;
import java.util.function.ToDoubleFunction;

public class SiegeActive {
    public final SiegeConfig config;

    public final ServerWorld world;
    public final GameSpace gameSpace;
    private static final int RESPAWN_DELAY_TICKS = 5 * 20;
    private static final int WARP_DELAY_TICKS = 2 * 20;

    final SiegeTeams teams;

    public final Object2ObjectMap<PlayerRef, SiegePlayer> participants;
    public final Map<PlayerRef, WarpingPlayer> warpingPlayers;

    final SiegeStageManager stageManager;
    final SiegeSidebar sidebar;
    final SiegeCaptureLogic captureLogic;
    final SiegeGateLogic gateLogic;
    public SiegeMap map;

    public static int TNT_GATE_DAMAGE = 10;

    private final static List<Item> PLANKS = List.of(
            SiegeKit.KitResource.PLANKS.attackerItem(),
            SiegeKit.KitResource.PLANKS.defenderItem()
    );

    private SiegeActive(ServerWorld world, GameActivity activity, SiegeMap map, SiegeConfig config,
                        GlobalWidgets widgets, Multimap<GameTeamKey, ServerPlayerEntity> players,
                        Map<PlayerRef, SiegeKit> kitSelections) {
        this.world = world;
        this.gameSpace = activity.getGameSpace();
        this.config = config;
        this.map = map;
        this.participants = new Object2ObjectOpenHashMap<>();
        this.warpingPlayers = new Object2ObjectOpenHashMap<>();

        this.teams = new SiegeTeams(activity);

        for (GameTeamKey key : players.keySet()) {
            for (ServerPlayerEntity player : players.get(key)) {
                var ref = PlayerRef.of(player);
                this.participants.put(ref, new SiegePlayer(SiegeTeams.byKey(key), kitSelections.get(ref)));
                this.teams.addPlayer(player, key);
            }
        }

        this.stageManager = new SiegeStageManager(this);
        this.sidebar = new SiegeSidebar(this, widgets);

        this.captureLogic = new SiegeCaptureLogic(this);
        this.gateLogic = new SiegeGateLogic(this);
    }

    public static void open(ServerWorld world, GameSpace gameSpace, SiegeMap map, SiegeConfig config,
                            Multimap<GameTeamKey, ServerPlayerEntity> players,
                            Map<PlayerRef, SiegeKit> kitSelections) {
        gameSpace.setActivity(activity -> {
            GlobalWidgets widgets = GlobalWidgets.addTo(activity);

            SiegeActive active = new SiegeActive(world, activity, map, config, widgets, players, kitSelections);

            activity.deny(GameRuleType.CRAFTING);
            activity.deny(GameRuleType.PORTALS);
            activity.allow(GameRuleType.PVP);
            activity.allow(GameRuleType.HUNGER);
            activity.allow(GameRuleType.INTERACTION);
            activity.allow(GameRuleType.FALL_DAMAGE);
            activity.allow(GameRuleType.PLACE_BLOCKS);
            activity.allow(GameRuleType.UNSTABLE_TNT);
            activity.listen(BlockDropItemsEvent.EVENT, active::onBlockDrop);

            activity.listen(GameActivityEvents.ENABLE, active::onOpen);
            activity.listen(GameActivityEvents.DISABLE, active::onClose);
            activity.listen(ItemThrowEvent.EVENT, active::onDropItem);
            activity.listen(EntityDropItemsEvent.EVENT, active::onEntityDropItem);
            activity.listen(BlockBreakEvent.EVENT, active::onBreakBlock);
            activity.listen(BlockPlaceEvent.BEFORE, active::onPlaceBlock);

            PlayerLimiter.addTo(activity, config.players().playerConfig());

            //activity.listen(GamePlayerEvents.OFFER, active::acceptPlayer);
            activity.listen(GamePlayerEvents.ACCEPT, active::acceptPlayer);
            activity.listen(GamePlayerEvents.REMOVE, active::removePlayer);
            activity.listen(BlockUseEvent.EVENT, active::onUseBlock);
            activity.listen(ExplosionDetonatedEvent.EVENT, active::onExplosion);
            activity.listen(BlockPunchEvent.EVENT, active::onHitBlock);
            activity.listen(ItemUseEvent.EVENT, active::onUseItem);

            activity.listen(GameActivityEvents.TICK, active::tick);

            activity.listen(PlayerDamageEvent.EVENT, active::onPlayerDamage);
            activity.listen(PlayerDeathEvent.EVENT, active::onPlayerDeath);
            activity.listen(ArrowFireEvent.EVENT, active::onPlayerFireArrow);
            activity.listen(PlayerAttackEntityEvent.EVENT, active::onAttackEntity);
            activity.listen(ProjectileHitEvent.ENTITY, active::onProjectileHitEntity);

            active.map.startGame(active);
        });
    }

    private DroppedItemsResult onEntityDropItem(LivingEntity livingEntity, List<ItemStack> itemStacks) {
        return DroppedItemsResult.deny();
    }

    private DroppedItemsResult onBlockDrop(Entity entity, ServerWorld world, BlockPos blockPos, BlockState blockState, List<ItemStack> itemStacks) {
        itemStacks.removeIf(stack -> !PLANKS.contains(stack.getItem()));
        return DroppedItemsResult.allow(itemStacks);
    }

    private EventResult onExplosion(Explosion explosion, List<BlockPos> blockPos) {
        if (!(explosion.getCausingEntity() instanceof ServerPlayerEntity player)) {
            return EventResult.PASS;
        }

        var participant = this.participant(player);

        if (participant == null) {
            return EventResult.PASS;
        }

        gate:
        for (SiegeGate gate : this.map.gates) {
            if (participant.team == gate.flag.team) {
                continue;
            }

            for (BlockPos pos : blockPos) {
                if (!gate.bashedOpen && gate.health > 0 && gate.portcullis.contains(pos)) {
                    gate.health = Math.max(0, gate.health - TNT_GATE_DAMAGE);
                    gate.timeOfLastBash = this.world.getTime();
                    gate.broadcastHealth(player, this, this.world);
                    break gate;
                }
            }
        }

        var dmg = ((ExplosionImplAccessor) explosion).getBehavior().calculateDamage(explosion, player, explosion.getPower());
        if (dmg > 0) {
            player.damage(this.world, Explosion.createDamageSource(this.world, null), dmg);
        }

        blockPos.removeIf(this.map::isProtectedBlock);

        return EventResult.PASS;
    }

    private EventResult onProjectileHitEntity(ProjectileEntity projectileEntity, EntityHitResult hitResult) {
        if (hitResult.getEntity() instanceof ServerPlayerEntity) {
            return EventResult.PASS;
        } else {
            return EventResult.DENY;
        }
    }

    private EventResult onAttackEntity(ServerPlayerEntity playerEntity, Hand hand, Entity entity, EntityHitResult entityHitResult) {
        if (entity instanceof ServerPlayerEntity) {
            return EventResult.PASS;
        } else {
            return EventResult.DENY;
        }
    }

    private EventResult onHitBlock(ServerPlayerEntity player, Direction direction, BlockPos pos) {
        SiegePlayer participant = this.participant(player);
        if (participant != null) {
            return this.gateLogic.maybeBash(pos, player, participant, this.world.getTime());
        } else {
            return EventResult.PASS;
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

    public PlayerSet team(GameTeam team) {
        var teamPlayers = new MutablePlayerSet(this.world.getServer());
        for (var entry : this.participants.entrySet()) {
            if (entry.getValue().team == team) {
                teamPlayers.add(entry.getKey());
            }
        }
        return teamPlayers;
    }

    public void showTitle(GameTeam team, Text title, @Nullable Text subtitle) {
        var stay = 5 * 20;
        var fadeIn = 5;
        var fadeOut = 10;

        if (subtitle != null) {
            this.team(team).showTitle(title, subtitle, fadeIn, stay, fadeOut);
        } else {
            this.team(team).showTitle(title, fadeIn, stay, fadeOut);
        }
    }

    private static void sendDescription(ServerPlayerEntity player, String base, int n) {
        var msg = Text.empty();
        for (int i = 1; i <= n; i++) {
            msg.append(Text.translatable(String.format("%s.desc.%s", base, i)).append(" "));
        }
        player.sendMessage(msg.formatted(Formatting.GOLD), false);
    }

    private void onOpen() {
        for (Map.Entry<PlayerRef, SiegePlayer> entry : this.participants.entrySet()) {
            entry.getKey().ifOnline(this.world, p -> {
                var participant = entry.getValue();
                if (this.config.recapture()) {
                    sendDescription(p, "game.siege.recapture", 3);
                }

                if (this.config.hasEnderPearl(participant.team)) {
                    sendDescription(p, "game.siege.enderpearl", 2);
                }

                if (this.config.capturingGiveTimeSecs() > 0) {
                    sendDescription(p, "game.siege.quick", 2);
                }

                this.spawnParticipant(p, this.map.getFirstSpawn(participant.team));
            });
        }

        this.showTitle(
                SiegeTeams.ATTACKERS,
                Text.translatable("game.siege.start.attacker.title")
                        .formatted(Formatting.RED),
                Text.translatable("game.siege.start.attacker.subtitle")
        );

        this.showTitle(
                SiegeTeams.DEFENDERS,
                Text.translatable("game.siege.start.defender.title")
                        .formatted(Formatting.AQUA),
                Text.translatable("game.siege.start.defender.subtitle")
        );

        if (SiegeMapLoader.loadRemote()) {
            var hint = Text.literal("[Siege] Loaded map from build server").formatted(Formatting.AQUA);
            this.gameSpace.getPlayers().sendMessage(hint);
        }

        this.stageManager.onOpen(this.world.getTime());
    }

    private void onClose() {
        for (SiegeFlag flag : this.map.flags) {
            flag.closeCaptureBar();
        }

        this.stageManager.closeTimerBar();
    }

    private JoinAcceptorResult acceptPlayer(JoinAcceptor offer) {
        return SiegeSpawnLogic.acceptPlayer(offer, this.world, this.map.waitingSpawn, offer.intent() == JoinIntent.PLAY ? GameMode.SURVIVAL : GameMode.SPECTATOR)
                .thenRunForEach((player, intent) -> {
                    if (intent == JoinIntent.PLAY) {
                        var spawn = this.getSpawnFor(player, this.world.getTime());
                        var pos = SiegeSpawnLogic.choosePos(world.getRandom(), spawn.spawn.bounds(), 0.5F);
                        player.teleport(world, pos.x, pos.y, pos.z, Set.of(), 0, 0, false);
                        if (!this.participants.containsKey(PlayerRef.of(player))) {
                            this.allocateParticipant(player);
                        }
                        this.completeParticipantSpawn(player);
                    }
                });
    }

    private void allocateParticipant(ServerPlayerEntity player) {
        GameTeamKey smallestTeam = this.teams.getSmallestTeam();
        SiegePlayer participant = new SiegePlayer(SiegeTeams.byKey(smallestTeam), null);
        this.participants.put(PlayerRef.of(player), participant);
        this.teams.addPlayer(player, smallestTeam);
    }

    private void removePlayer(ServerPlayerEntity player) {
        SiegePlayer participant = this.participants.remove(PlayerRef.of(player));
        if (participant != null) {
            this.teams.removePlayer(player, participant.team.key());
        }
    }

    private EventResult onDropItem(PlayerEntity player, int slot, ItemStack stack) {
        return EventResult.DENY;
    }

    private EventResult onPlaceBlock(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos blockPos,
            BlockState blockState,
            ItemUsageContext ctx
    ) {
        SiegePlayer participant = this.participant(player);
        if (participant == null) {
            return EventResult.DENY;
        }

        if (participant.kit == SiegeKit.ENGINEER) {
            // TNT may be placed anyway
            for (BlockBounds noBuildRegion : this.map.noBuildRegions) {
                if (noBuildRegion.contains(blockPos)) {
                    return EventResult.DENY;
                }
            }

            if (this.map.isProtectedBlock(blockPos.asLong())) {
                return EventResult.DENY;
            }

            return this.gateLogic.maybeBraceGate(blockPos, participant, player, ctx, this.world.getTime());
        } else {
            return EventResult.DENY;
        }
    }

    private EventResult onBreakBlock(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        if (this.map.isProtectedBlock(pos.asLong())) {
            return EventResult.DENY;
        }
        return EventResult.PASS;
    }

    private ActionResult onUseBlock(ServerPlayerEntity player, Hand hand, BlockHitResult hitResult) {
        BlockPos pos = hitResult.getBlockPos();
        if (pos == null) {
            return ActionResult.PASS;
        }

        SiegePlayer participant = this.participant(player);
        Item inHand = player.getStackInHand(hand).getItem();
        if (participant != null) {
            pos.offset(hitResult.getSide());
            BlockState state = this.world.getBlockState(pos);
            if (state.getBlock() instanceof EnderChestBlock) {
                MutableText result = participant.kit.restock(player, participant, this.world.getTime()).copy();
                player.sendMessage(result.formatted(Formatting.BOLD), true);
                return ActionResult.FAIL;
            } else if (state.getBlock() instanceof DoorBlock) {
                return ActionResult.PASS;
            } else if (state.getBlock() instanceof BlockWithEntity) {
                return ActionResult.FAIL;
            } else if (SiegeGateLogic.canUseToBash(inHand)) {
                if (this.gateLogic.maybeBash(pos, player, participant, this.world.getTime()) == EventResult.DENY) {
                    return ActionResult.FAIL;
                }
            }

            // Disable log stripping
            if (inHand instanceof AxeItem) {
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        }

        return ActionResult.PASS;
    }

    private ActionResult onUseItem(ServerPlayerEntity player, Hand hand) {
        SiegePlayer participant = this.participant(player);
        ItemStack stack = player.getStackInHand(hand);
        if (participant != null) {
            ItemCooldownManager cooldownManager = player.getItemCooldownManager();

            if (cooldownManager.isCoolingDown(stack)) {
                return ActionResult.FAIL;
            }

            if (stack.isOf(Items.ENDER_PEARL)) {
                SimpleGui ui = WarpSelectionUi.createFlagWarp(player, this.map, participant.team, selectedFlag -> {
                    cooldownManager.set(stack, 10 * 20);
                    cooldownManager.set(SiegeKit.KIT_SELECT_ITEM.getDefaultStack(), SiegeKit.KIT_SWAP_COOLDOWN);

                    this.warpingPlayers.put(
                            PlayerRef.of(player),
                            new WarpingPlayer(
                                    player,
                                    selectedFlag.getRespawnFor(participant.team),
                                    this.world.getTime(),
                                    null
                            )
                    );

                    player.sendMessage(Text.literal(String.format("Warping to %s... hold still!", selectedFlag.name))
                            .formatted(Formatting.GREEN), true);
                    player.playSoundToPlayer(SoundEvents.ENTITY_ENDER_PEARL_THROW, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                });

                ui.open();

                return ActionResult.FAIL;
            } else if (stack.isOf(SiegeKit.KIT_SELECT_ITEM)) {
                SimpleGui ui = WarpSelectionUi.createKitSelect(player, participant.kit, selectedKit -> {
                    long time = player.getWorld().getTime();
                    var spawn = this.getSpawnFor(player, time);

                    cooldownManager.set(Items.ENDER_PEARL.getDefaultStack(), 10 * 20);
                    cooldownManager.set(SiegeKit.KIT_SELECT_ITEM.getDefaultStack(), 10 * 20);
                    this.warpingPlayers.put(
                            PlayerRef.of(player),
                            new WarpingPlayer(player, spawn.spawn, this.world.getTime(), selectedKit)
                    );

                    var msg = Text.literal("Respawning as ").append(selectedKit.getName());

                    if (spawn.flag != null) {
                        msg.append(" at ").append(spawn.flag.name);
                    }

                    player.sendMessage(msg.append("... hold still!").formatted(Formatting.GREEN), true);
                    player.playSoundToPlayer(SoundEvents.ENTITY_ENDER_PEARL_THROW, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                });

                ui.open();

                return ActionResult.FAIL;
            } else if (stack.isOf(SiegeItems.HORN)) {
                return SiegeHorn.onUse(this, player, participant, stack, hand);
            }
        }

        return ActionResult.PASS;
    }

    private EventResult onPlayerDamage(ServerPlayerEntity player, DamageSource source, float v) {
        SiegePlayer participant = this.participant(player);
        long time = this.world.getTime();

        if (participant != null && this.world.getTime() < participant.timeOfSpawn + RESPAWN_DELAY_TICKS && !participant.attackedThisLife) {
            return EventResult.DENY;
        }

        if (participant != null && source.getAttacker() != null && source.getAttacker() instanceof ServerPlayerEntity) {
            PlayerRef attacker = PlayerRef.of((ServerPlayerEntity) source.getAttacker());
            participant.lastTimeWasAttacked = new AttackRecord(attacker, time);

            SiegePlayer attackerParticipant = this.participant(attacker);
            if (attackerParticipant != null) {
                attackerParticipant.attackedThisLife = true;
            }
        }

        var ref = PlayerRef.of(player);
        var warping = this.warpingPlayers.get(ref);
        if (warping != null) {
            this.warpingPlayers.remove(ref);
            player.sendMessage(Text.literal("Cancelled because you took damage!").formatted(Formatting.RED), true);
            player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.NEUTRAL, 1.0F, 1.0F);
        }

        return EventResult.DENY;
    }

    private EventResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        MutableText deathMessage = this.getDeathMessageAndIncStats(player, source);
        this.gameSpace.getPlayers().sendMessage(deathMessage.formatted(Formatting.GRAY));

        this.spawnDeadParticipant(player);

        return EventResult.DENY;
    }

    private MutableText getDeathMessageAndIncStats(ServerPlayerEntity player, DamageSource source) {
        SiegePlayer participant = this.participant(player);
        var world = this.world;
        long time = world.getTime();

        MutableText eliminationMessage = Text.literal(" was ");
        SiegePlayer attacker = null;

        if (source.isIn(DamageTypeTags.IS_EXPLOSION)) {
            eliminationMessage.append("blown up");
        } else {
            eliminationMessage.append("killed");
        }

        eliminationMessage.append(" by ");

        if (source.getAttacker() != null) {
            eliminationMessage.append(source.getAttacker().getDisplayName());

            if (source.getAttacker() instanceof ServerPlayerEntity) {
                attacker = this.participant((ServerPlayerEntity) source.getAttacker());
            }
        } else if (participant != null && participant.attacker(time, world) != null) {
            eliminationMessage.append(participant.attacker(time, world).getDisplayName());
            attacker = this.participant(participant.attacker(time, world));
        } else if (source.isIn(DamageTypeTags.IS_DROWNING)) {
            eliminationMessage.append("forgetting to just keep swimming");
        } else {
            eliminationMessage = Text.literal(" died");
        }

        if (attacker != null) {
            attacker.kills += 1;
        }

        if (participant != null) {
            participant.deaths += 1;
        }

        return Text.empty().append(player.getDisplayName()).append(eliminationMessage);
    }

    private EventResult onPlayerFireArrow(
            ServerPlayerEntity user,
            ItemStack tool,
            ArrowItem arrowItem,
            int remainingUseTicks,
            PersistentProjectileEntity projectile
    ) {
        projectile.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
        return EventResult.DENY;
    }

    private void spawnDeadParticipant(ServerPlayerEntity player) {
        player.getEnderChestInventory().clear();
        player.changeGameMode(GameMode.SPECTATOR);
        SiegePlayer participant = this.participant(player);

        if (participant != null) {
            participant.timeOfDeath = this.world.getTime();
            participant.kit.returnResources(player, participant);
        }
    }

    private void spawnParticipant(ServerPlayerEntity player, @Nullable SiegeSpawn spawn) {
        if (spawn == null) {
            spawn = this.getSpawnFor(player, this.world.getTime()).spawn;
        }

        this.stageManager.timerBar.addPlayer(player);
        SiegeSpawnLogic.resetPlayer(player, GameMode.SURVIVAL);
        SiegeSpawnLogic.spawnPlayer(player, spawn, this.world);

        this.completeParticipantSpawn(player);
    }

    private void completeParticipantSpawn(ServerPlayerEntity player) {
        player.getInventory().clear();
        player.getEnderChestInventory().clear();
        SiegePlayer participant = this.participant(player);
        assert participant != null; // spawnParticipant should only be called on a participant

        var time = this.world.getTime();
        participant.timeOfSpawn = time;
        participant.kit.equipPlayer(player, participant, this.config, time);
    }

    private SiegeSpawnResult getSpawnFor(ServerPlayerEntity player, long time) {
        GameTeam team = this.getTeamFor(player);
        if (team == null) {
            return new SiegeSpawnResult(null, this.map.waitingSpawn);
        }

        SiegeSpawnResult respawn = new SiegeSpawnResult(null, this.map.waitingSpawn);
        double minDistance = Double.MAX_VALUE;

        for (SiegeFlag flag : this.map.flags) {
            SiegeSpawn flagRespawn = flag.getRespawnFor(team);
            if (flagRespawn != null && flag.team == team) {
                double distance = player.squaredDistanceTo(flagRespawn.bounds().center());
                boolean frontLine = flag.isFrontLine(time);

                if ((distance < minDistance && frontLine == respawn.isFrontLine(time)) ||
                        (frontLine && !respawn.isFrontLine(time))) {
                    respawn.setFlag(flag, flagRespawn);
                    minDistance = distance;
                }
            }
        }

        return respawn;
    }

    private void tick() {
        long time = this.world.getTime();

        SiegeStageManager.TickResult result = this.stageManager.tick(time);
        if (!result.continueGame()) {
            switch (result) {
                case ATTACKERS_WIN -> this.broadcastWin(SiegeTeams.ATTACKERS);
                case DEFENDERS_WIN -> this.broadcastWin(SiegeTeams.DEFENDERS);
                case GAME_CLOSED -> this.gameSpace.close(GameCloseReason.FINISHED);
            }

            return;
        }

        if (time % 20 == 0) {
            this.captureLogic.tick(this.world, 20);
            this.gateLogic.tick();
            this.sidebar.update(time);
            this.tickResources(time);
        }

        this.tickWarpingPlayers(time);

        this.tickDead(this.world, time);
    }

    @Nullable
    private GameTeam getTeamFor(ServerPlayerEntity player) {
        SiegePlayer participant = this.participant(player);
        return participant != null ? participant.team : null;
    }

    private void tickWarpingPlayers(long time) {
        this.warpingPlayers.values().removeIf(warpingPlayer -> {
            ServerPlayerEntity player = warpingPlayer.player.getEntity(this.world);
            var participant = this.participant(player);

            if (player == null || participant == null) {
                return true;
            }

            if (this.world.getTime() - warpingPlayer.startTime > WARP_DELAY_TICKS) {
                SiegeSpawn respawn = warpingPlayer.destination;
                assert respawn != null; // TODO remove restriction
                Vec3d pos = SiegeSpawnLogic.choosePos(player.getRandom(), respawn.bounds(), 0.5f);
                player.teleport(this.world, pos.x, pos.y, pos.z, Set.of(), respawn.yaw(), 0.0F, false);
                player.playSoundToPlayer(SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.NEUTRAL, 1.0F, 1.0F);

                if (warpingPlayer.newKit != null) {
                    warpingPlayer.newKit.equipPlayer(player, participant, this.config, time);
                }
                return true;
            }

            // Set X and Y as relative so it will send 0 change when we pass yaw (yaw - yaw = 0) and pitch
            Set<PositionFlag> flags = ImmutableSet.of(PositionFlag.X_ROT, PositionFlag.Y_ROT);

            // Teleport without changing the pitch and yaw
            player.networkHandler.requestTeleport(new PlayerPosition(warpingPlayer.pos, Vec3d.ZERO, 0, 0), flags);

            return false;
        });
    }

    private void tickResources(long time) {
        for (SiegePersonalResource resource : SiegePersonalResource.values()) {
            if (time % (resource.refreshSecs * 20L) == 0) {
                for (SiegePlayer player : this.participants.values()) {
                    player.incrementResource(resource, 1);
                }
            }
        }
    }

    private void tickDead(ServerWorld world, long time) {
        for (Object2ObjectMap.Entry<PlayerRef, SiegePlayer> entry : Object2ObjectMaps.fastIterable(this.participants)) {
            PlayerRef ref = entry.getKey();
            SiegePlayer state = entry.getValue();
            ref.ifOnline(world, p -> {
                if (p.isSpectator()) {
                    int sec = 5 - (int) Math.floor((time - state.timeOfDeath) / 20.0f);

                    if (sec > 0 && (time - state.timeOfDeath) % 20 == 0) {
                        Text text = Text.literal(String.format("Respawning in %ds", sec)).formatted(Formatting.BOLD);
                        p.sendMessage(text, true);
                    }

                    if (time - state.timeOfDeath > RESPAWN_DELAY_TICKS) {
                        this.spawnParticipant(p, null);
                    }
                }
            });
        }
    }

    private void broadcastWin(GameTeam winningTeam) {
        for (Map.Entry<PlayerRef, SiegePlayer> entry : this.participants.entrySet()) {
            entry.getKey().ifOnline(this.gameSpace.getServer(), player -> {
                if (entry.getValue().team == winningTeam && winningTeam == SiegeTeams.DEFENDERS) {
                    player.playSoundToPlayer(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.0F, 1.0F);
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, 10, 0, false, false, true));
                }
            });
        }

        PlayerSet players = this.gameSpace.getPlayers();
        SiegeDialogueLogic.broadcastWin(this, winningTeam);

        Optional<BestPlayer> mostKills = this.getPlayerWithHighest(p -> p.kills);
        Optional<BestPlayer> highestKd = this.getPlayerWithHighest(p -> (double) p.kills / Math.max(1, p.deaths));
        Optional<BestPlayer> mostCaptures = this.getPlayerWithHighest(p -> p.captures);
        Optional<BestPlayer> mostSecures = this.getPlayerWithHighest(p -> p.secures);

        Formatting colour = Formatting.GOLD;

        mostKills.ifPresent(p -> players.sendMessage(Text.literal(String.format("Most kills - %s with %d", p.name, (int) p.score)).formatted(colour)));
        highestKd.ifPresent(p -> players.sendMessage(Text.literal(String.format("Highest KD - %s with %.2f", p.name, p.score)).formatted(colour)));
        mostCaptures.ifPresent(p -> players.sendMessage(Text.literal(String.format("Most captures - %s with %d", p.name, (int) p.score)).formatted(colour)));
        mostSecures.ifPresent(p -> players.sendMessage(Text.literal(String.format("Most secures - %s with %d", p.name, (int) p.score)).formatted(colour)));

        int attacker_kills = 0;
        int defender_kills = 0;
        int attacker_deaths = 0;
        int defender_deaths = 0; // separate because other deaths exist

        for (Map.Entry<PlayerRef, SiegePlayer> entry : this.participants.entrySet()) {
            ServerPlayerEntity p = entry.getKey().getEntity(this.world);

            if (p != null) {
                int kills = entry.getValue().kills;
                int deaths = entry.getValue().deaths;

                double kd = (double) kills / Math.max(1, deaths);
                MutableText text = Text.literal("\nYour statistics:\n")
                        .append(String.format("Kills - %d\n", kills))
                        .append(String.format("Deaths - %d\n", deaths))
                        .append(String.format("K/D - %.2f\n", kd));

                if (entry.getValue().team == SiegeTeams.DEFENDERS) {
                    text.append(String.format("Secures - %d", entry.getValue().secures));
                } else {
                    text.append(String.format("Captures - %d", entry.getValue().captures));
                }

                p.sendMessage(text.formatted(colour), false);

                if (entry.getValue().team == SiegeTeams.DEFENDERS) {
                    defender_kills += kills;
                    defender_deaths += deaths;
                } else {
                    attacker_kills += kills;
                    attacker_deaths += deaths;
                }
            }
        }

        double attacker_kd = (double) attacker_kills / Math.max(attacker_deaths, 1);
        double defender_kd = (double) defender_kills / Math.max(defender_deaths, 1);

        Formatting bold = Formatting.BOLD;
        players.sendMessage(Text.literal(String.format("Attacker kills - %d", attacker_kills)).formatted(colour).formatted(bold));
        players.sendMessage(Text.literal(String.format("Attacker deaths - %d", attacker_deaths)).formatted(colour).formatted(bold));
        players.sendMessage(Text.literal(String.format("Attacker K/D - %.2f", attacker_kd)).formatted(colour).formatted(bold));
        players.sendMessage(Text.literal(String.format("Defender kills - %d", defender_kills)).formatted(colour).formatted(bold));
        players.sendMessage(Text.literal(String.format("Defender deaths - %d", defender_deaths)).formatted(colour).formatted(bold));
        players.sendMessage(Text.literal(String.format("Defender K/D - %.2f", defender_kd)).formatted(colour).formatted(bold));
    }

    private Optional<BestPlayer> getPlayerWithHighest(ToDoubleFunction<SiegePlayer> getter) {
        return this.participants.entrySet()
                .stream()
                .max(Comparator.comparingDouble(e -> getter.applyAsDouble(e.getValue())))
                .map(e -> {
                    ServerPlayerEntity p = e.getKey().getEntity(this.world);

                    if (p == null) {
                        return null;
                    }

                    return new BestPlayer(p.getGameProfile().getName(), getter.applyAsDouble(e.getValue()));
                });
    }

    static class SiegeSpawnResult {
        @Nullable
        SiegeFlag flag;
        SiegeSpawn spawn;

        public SiegeSpawnResult(@Nullable SiegeFlag flag, SiegeSpawn spawn) {
            this.flag = flag;
            this.spawn = spawn;
        }

        public void setFlag(SiegeFlag flag, SiegeSpawn spawn) {
            this.flag = flag;
            this.spawn = spawn;
        }

        public boolean isFrontLine(long time) {
            return this.flag != null && this.flag.isFrontLine(time);
        }
    }

    static class BestPlayer {
        String name;
        double score;

        public BestPlayer(String name, double score) {
            this.name = name;
            this.score = score;
        }
    }
}
