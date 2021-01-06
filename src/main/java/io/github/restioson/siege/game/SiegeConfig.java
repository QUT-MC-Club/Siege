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
            Codec.BOOL.fieldOf("recapture").orElse(false).forGetter(config -> config.recapture),
            Codec.BOOL.fieldOf("defender_ender_pearl").orElse(false).forGetter(config -> config.defenderEnderPearl)
    ).apply(instance, SiegeConfig::new));

    public final PlayerConfig playerConfig;
    public final SiegeMapConfig mapConfig;
    public final int timeLimitMins;
    public boolean recapture;
    public boolean defenderEnderPearl;

    public SiegeConfig(PlayerConfig players, SiegeMapConfig mapConfig, int timeLimitMins, boolean recapture, boolean defenderEnderPearl) {
        this.playerConfig = players;
        this.mapConfig = mapConfig;
        this.timeLimitMins = timeLimitMins;
        this.recapture = recapture;
        this.defenderEnderPearl = defenderEnderPearl;
    }
}
