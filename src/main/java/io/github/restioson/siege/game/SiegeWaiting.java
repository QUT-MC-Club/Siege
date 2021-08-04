package io.github.restioson.siege.game;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.github.restioson.siege.game.active.SiegeActive;
import io.github.restioson.siege.game.map.SiegeMap;
import io.github.restioson.siege.game.map.SiegeMapLoader;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
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
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class SiegeWaiting {
    private final ServerWorld world;
    private final GameSpace gameSpace;
    private final SiegeMap map;
    private final SiegeConfig config;

    private final TeamSelectionLobby teamSelection;

    private SiegeWaiting(ServerWorld world, GameSpace gameSpace, SiegeMap map, SiegeConfig config, TeamSelectionLobby teamSelection) {
        this.world = world;
        this.gameSpace = gameSpace;
        this.map = map;
        this.config = config;
        this.teamSelection = teamSelection;
    }

    public static GameOpenProcedure open(GameOpenContext<SiegeConfig> context) {
        var config = context.config();
        SiegeMapLoader generator = new SiegeMapLoader(config.map());
        SiegeMap map = generator.create(context.server());

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
        });
    }

    private GameResult requestStart() {
        Multimap<GameTeamKey, ServerPlayerEntity> players = HashMultimap.create();
        this.teamSelection.allocate(this.gameSpace.getPlayers(), players::put);

        SiegeActive.open(this.world, this.gameSpace, this.map, this.config, players);

        return GameResult.ok();
    }

    private PlayerOfferResult offerPlayer(PlayerOffer offer) {
        return SiegeSpawnLogic.acceptPlayer(offer, this.world, this.map.waitingSpawn, GameMode.ADVENTURE);
    }

    private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        player.setHealth(20.0F);
        SiegeSpawnLogic.resetPlayer(player, GameMode.ADVENTURE);
        SiegeSpawnLogic.spawnPlayer(player, this.map.waitingSpawn, this.world);
        return ActionResult.FAIL;
    }
}
