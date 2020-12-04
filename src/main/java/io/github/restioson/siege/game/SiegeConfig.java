package io.github.restioson.siege.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.restioson.siege.game.map.SiegeMapConfig;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;

public class SiegeConfig {
    public static final Codec<SiegeConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PlayerConfig.CODEC.fieldOf("players").forGetter(config -> config.playerConfig),
            SiegeMapConfig.CODEC.fieldOf("map").forGetter(config -> config.mapConfig),
            Codec.INT.fieldOf("time_limit_mins").forGetter(config -> config.timeLimitMins),
            Codec.BOOL.fieldOf("recapture").forGetter(config -> config.recapture)
    ).apply(instance, SiegeConfig::new));

    public final PlayerConfig playerConfig;
    public final SiegeMapConfig mapConfig;
    public final int timeLimitMins;
    public boolean recapture;

    public SiegeConfig(PlayerConfig players, SiegeMapConfig mapConfig, int timeLimitMins, boolean recapture) {
        this.playerConfig = players;
        this.mapConfig = mapConfig;
        this.timeLimitMins = timeLimitMins;
        this.recapture = recapture;
    }
}
