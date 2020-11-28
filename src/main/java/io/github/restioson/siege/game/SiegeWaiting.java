package io.github.restioson.siege.game;

import io.github.restioson.siege.game.map.SiegeMap;
import io.github.restioson.siege.game.map.SiegeMapGenerator;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.BubbleWorldConfig;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.RequestStartListener;

public class SiegeWaiting {
    private final GameSpace gameSpace;
    private final SiegeMap map;
    private final SiegeConfig config;

    private SiegeWaiting(GameSpace gameSpace, SiegeMap map, SiegeConfig config) {
        this.gameSpace = gameSpace;
        this.map = map;
        this.config = config;
    }

    public static GameOpenProcedure open(GameOpenContext<SiegeConfig> context) {
        SiegeMapGenerator generator = new SiegeMapGenerator(context.getConfig().mapConfig);
        SiegeMap map = generator.create();

        BubbleWorldConfig worldConfig = new BubbleWorldConfig()
                .setGenerator(map.asGenerator(context.getServer()))
                .setDefaultGameMode(GameMode.SPECTATOR);

        return context.createOpenProcedure(worldConfig, game -> {
            SiegeWaiting waiting = new SiegeWaiting(game.getSpace(), map, context.getConfig());

            GameWaitingLobby.applyTo(game, context.getConfig().playerConfig);

            game.on(RequestStartListener.EVENT, waiting::requestStart);
            game.on(PlayerAddListener.EVENT, waiting::addPlayer);
            game.on(PlayerDeathListener.EVENT, waiting::onPlayerDeath);
        });
    }

    private StartResult requestStart() {
        SiegeActive.open(this.gameSpace, this.map, this.config);
        return StartResult.OK;
    }

    private void addPlayer(ServerPlayerEntity player) {
        this.spawnPlayer(player);
    }

    private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        player.setHealth(20.0f);
        this.spawnPlayer(player);
        return ActionResult.FAIL;
    }

    private void spawnPlayer(ServerPlayerEntity player) {
        SiegeSpawnLogic.resetPlayer(player, GameMode.ADVENTURE);
        SiegeSpawnLogic.spawnPlayer(player, this.map.waitingSpawn, this.gameSpace.getWorld());
    }
}
