package io.github.restioson.siege.game;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import eu.pb4.sgui.api.gui.SimpleGui;
import io.github.restioson.siege.game.active.SiegeActive;
import io.github.restioson.siege.game.active.WarpSelectionUi;
import io.github.restioson.siege.game.map.SiegeMap;
import io.github.restioson.siege.game.map.SiegeMapLoader;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameResult;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.game.common.team.TeamSelectionLobby;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.util.PlayerRef;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

import java.util.Map;

public class SiegeWaiting {
    private final ServerWorld world;
    private final GameSpace gameSpace;
    private final SiegeMap map;
    private final SiegeConfig config;

    private final Map<PlayerRef, SiegeKit> kitSelections;
    private final TeamSelectionLobby teamSelection;

    private SiegeWaiting(ServerWorld world, GameSpace gameSpace, SiegeMap map, SiegeConfig config, TeamSelectionLobby teamSelection) {
        this.world = world;
        this.gameSpace = gameSpace;
        this.map = map;
        this.config = config;
        this.teamSelection = teamSelection;
        this.kitSelections = new Object2ObjectOpenHashMap<>();
    }

    public static GameOpenProcedure open(GameOpenContext<SiegeConfig> context) {
        var config = context.config();
        SiegeMap map = SiegeMapLoader.load(context.server(), config.map());

        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setGenerator(map.asGenerator(context.server()))
                .setTimeOfDay(map.time);

        return context.openWithWorld(worldConfig, (activity, world) -> {
            GameWaitingLobby.addTo(activity, config.players());

            TeamSelectionLobby teamSelection = TeamSelectionLobby.addTo(activity, SiegeTeams.TEAMS);

            SiegeWaiting waiting = new SiegeWaiting(world, activity.getGameSpace(), map, config, teamSelection);

            activity.listen(GameActivityEvents.REQUEST_START, waiting::requestStart);
            activity.listen(GamePlayerEvents.OFFER, waiting::offerPlayer);
            activity.listen(PlayerDeathEvent.EVENT, waiting::onPlayerDeath);
            activity.listen(ItemUseEvent.EVENT, waiting::onUseItem);
            activity.listen(GamePlayerEvents.ADD, waiting::onAddPlayer);
        });
    }

    private void onAddPlayer(ServerPlayerEntity player) {
        player.getInventory().offerOrDrop(SiegeKit.kitSelectItemStack());
    }

    private TypedActionResult<ItemStack> onUseItem(ServerPlayerEntity player, Hand hand) {
        var stack = player.getStackInHand(hand);

        if (stack.getItem() == SiegeKit.KIT_SELECT_ITEM) {
            var ref = PlayerRef.of(player);
            SimpleGui ui = WarpSelectionUi.createKitSelect(player, this.kitSelections.get(ref), selectedKit -> {
                this.kitSelections.put(ref, selectedKit);
                var msg = Text.translatable("game.siege.kit.selected")
                        .append(" ")
                        .append(selectedKit.getName())
                        .formatted(Formatting.GREEN);
                player.sendMessage((msg), true);
                player.playSound(SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, SoundCategory.NEUTRAL, 1.0F, 1.0F);
            });

            ui.open();
        }

        return TypedActionResult.fail(stack);
    }

    private GameResult requestStart() {
        Multimap<GameTeamKey, ServerPlayerEntity> players = HashMultimap.create();
        this.teamSelection.allocate(this.gameSpace.getPlayers(), players::put);

        SiegeActive.open(this.world, this.gameSpace, this.map, this.config, players, this.kitSelections);

        return GameResult.ok();
    }

    private PlayerOfferResult offerPlayer(PlayerOffer offer) {
        var res = SiegeSpawnLogic.acceptPlayer(offer, this.world, this.map.waitingSpawn, GameMode.ADVENTURE);
        offer.player().getInventory().offerOrDrop(SiegeKit.kitSelectItemStack());
        return res;
    }

    private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        player.setHealth(20.0F);
        SiegeSpawnLogic.resetPlayer(player, GameMode.ADVENTURE);
        SiegeSpawnLogic.spawnPlayer(player, this.map.waitingSpawn, this.world);
        return ActionResult.FAIL;
    }
}
