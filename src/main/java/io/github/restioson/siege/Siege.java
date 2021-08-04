package io.github.restioson.siege;

import io.github.restioson.siege.game.SiegeConfig;
import io.github.restioson.siege.game.SiegeWaiting;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nucleoid.plasmid.game.GameType;

public class Siege implements ModInitializer {
    public static final String ID = "siege";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    @Override
    public void onInitialize() {
        GameType.register(
                new Identifier(ID, "siege"),
                SiegeConfig.CODEC,
                SiegeWaiting::open
        );
    }
}
