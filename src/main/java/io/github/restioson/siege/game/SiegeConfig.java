package io.github.restioson.siege.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.restioson.siege.game.map.SiegeMapConfig;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeam;

public record SiegeConfig(
        WaitingLobbyConfig players,
        SiegeMapConfig map,
        int timeLimitMins,
        int capturingGiveTimeSecs,
        boolean recapture,
        boolean defenderEnderPearl,
        boolean attackerEnderPearl
) {
    public static final MapCodec<SiegeConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            WaitingLobbyConfig.CODEC.fieldOf("players").forGetter(config -> config.players),
            SiegeMapConfig.CODEC.fieldOf("map").forGetter(config -> config.map),
            Codec.INT.fieldOf("time_limit_mins").forGetter(config -> config.timeLimitMins),
            Codec.INT.fieldOf("capturing_give_time_secs").orElse(0).forGetter(config -> config.capturingGiveTimeSecs),
            Codec.BOOL.fieldOf("recapture").orElse(false).forGetter(config -> config.recapture),
            Codec.BOOL.fieldOf("defender_ender_pearl").orElse(false).forGetter(config -> config.defenderEnderPearl),
            Codec.BOOL.fieldOf("attacker_ender_pearl").orElse(false).forGetter(config -> config.attackerEnderPearl)
    ).apply(instance, SiegeConfig::new));

    public boolean hasEnderPearl(GameTeam team) {
        return (team == SiegeTeams.ATTACKERS && this.attackerEnderPearl) ||
                (team == SiegeTeams.DEFENDERS && this.defenderEnderPearl);
    }

    public String giveTimeFormatted() {
        return String.format(
                "%02d:%02d",
                Math.floorDiv(this.capturingGiveTimeSecs, 60),
                this.capturingGiveTimeSecs % 60
        );
    }
}
