package io.github.restioson.siege.game.map;

import net.minecraft.text.LiteralText;
import net.minecraft.world.biome.BiomeKeys;
import xyz.nucleoid.plasmid.game.GameOpenException;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.map.template.MapTemplateSerializer;

import java.io.IOException;

public class SiegeMapGenerator {

    private final SiegeMapConfig config;

    public SiegeMapGenerator(SiegeMapConfig config) {
        this.config = config;
    }

    public SiegeMap create() throws GameOpenException {
        try {
            MapTemplate template = MapTemplateSerializer.INSTANCE.loadFromResource(this.config.id);

            SiegeMap map = new SiegeMap(template, this.config.attackerSpawnAngle);
            template.setBiome(BiomeKeys.PLAINS);

            return map;
        } catch (IOException e) {
            throw new GameOpenException(new LiteralText("Failed to load map"), e);
        }
    }
}
