package io.github.restioson.siege;

import net.fabricmc.api.ModInitializer;
import xyz.nucleoid.plasmid.game.GameType;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.github.restioson.siege.game.SiegeConfig;
import io.github.restioson.siege.game.SiegeWaiting;

public class Siege implements ModInitializer {

    public static final String ID = "siege";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public static final GameType<SiegeConfig> TYPE = GameType.register(
            new Identifier(ID, "siege"),
            SiegeWaiting::open,
            SiegeConfig.CODEC
    );

    @Override
    public void onInitialize() {}
}
