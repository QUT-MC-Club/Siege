package io.github.restioson.siege.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.restioson.siege.game.map.SiegeMapConfig;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;

public record SiegeConfig(
        PlayerConfig players,
        SiegeMapConfig map,
        int timeLimitMins,
        int capturingGiveTimeSecs,
        boolean recapture,
        boolean defenderEnderPearl
) {
    public static final Codec<SiegeConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PlayerConfig.CODEC.fieldOf("players").forGetter(config -> config.players),
            SiegeMapConfig.CODEC.fieldOf("map").forGetter(config -> config.map),
            Codec.INT.fieldOf("time_limit_mins").forGetter(config -> config.timeLimitMins),
            Codec.INT.fieldOf("capturing_give_time_secs").orElse(0).forGetter(config -> config.capturingGiveTimeSecs),
            Codec.BOOL.fieldOf("recapture").orElse(false).forGetter(config -> config.recapture),
            Codec.BOOL.fieldOf("defender_ender_pearl").orElse(false).forGetter(config -> config.defenderEnderPearl)
    ).apply(instance, SiegeConfig::new));
}
