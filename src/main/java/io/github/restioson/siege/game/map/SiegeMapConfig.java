package io.github.restioson.siege.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;

public class SiegeMapConfig {
    public static final Codec<SiegeMapConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(config -> config.id),
            Codec.INT.fieldOf("attacker_spawn_angle").forGetter(config -> config.attackerSpawnAngle)
    ).apply(instance, SiegeMapConfig::new));

    public final Identifier id;
    public final int attackerSpawnAngle;

    public SiegeMapConfig(Identifier id, int spawnAngle) {
        this.id = id;
        this.attackerSpawnAngle = spawnAngle;
    }
}
