package io.github.restioson.siege.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;
import io.github.restioson.siege.game.map.SiegeMapConfig;

public class SiegeConfig {
    public static final Codec<SiegeConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PlayerConfig.CODEC.fieldOf("players").forGetter(config -> config.playerConfig),
            SiegeMapConfig.CODEC.fieldOf("map").forGetter(config -> config.mapConfig),
            Codec.INT.fieldOf("time_limit_mins").forGetter(config -> config.timeLimitMins)
    ).apply(instance, SiegeConfig::new));

    public final PlayerConfig playerConfig;
    public final SiegeMapConfig mapConfig;
    public final int timeLimitMins;

    public SiegeConfig(PlayerConfig players, SiegeMapConfig mapConfig, int timeLimitMins) {
        this.playerConfig = players;
        this.mapConfig = mapConfig;
        this.timeLimitMins = timeLimitMins;
    }
}
