package io.github.restioson.siege.game;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.util.BlockBounds;

public class SiegeSpawnLogic {
    public static void resetPlayer(ServerPlayerEntity player, GameMode gameMode) {
        player.setGameMode(gameMode);
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0.0f;
    }

    public static void spawnPlayer(ServerPlayerEntity player, BlockBounds bounds, ServerWorld world) {
        BlockPos min = bounds.getMin();
        BlockPos max = bounds.getMax();

        double x = MathHelper.nextDouble(player.getRandom(), min.getX(), max.getX());
        double z = MathHelper.nextDouble(player.getRandom(), min.getZ(), max.getZ());
        double y = min.getY() + 0.5;

        player.teleport(world, x, y, z, 0.0F, 0.0F);
    }
}
