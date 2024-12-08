package io.github.restioson.siege;

import io.github.restioson.siege.game.SiegeConfig;
import io.github.restioson.siege.game.SiegeWaiting;
import io.github.restioson.siege.item.SiegeItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nucleoid.plasmid.api.game.GameType;

public class Siege implements ModInitializer {
    public static final String ID = "siege";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    @Override
    public void onInitialize() {
        SiegeItems.register();

        GameType.register(
                Identifier.of(ID, "siege"),
                SiegeConfig.CODEC,
                SiegeWaiting::open
        );
    }
}
