package io.github.restioson.siege.game;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.util.BlockBounds;

import java.util.Random;

public class SiegeSpawnLogic {
    public static void resetPlayer(ServerPlayerEntity player, GameMode gameMode) {
        player.setGameMode(gameMode);
        player.setVelocity(Vec3d.ZERO);
        player.getHungerManager().setFoodLevel(20);
        player.fallDistance = 0.0f;
    }

    public static Vec3d choosePos(Random random, BlockBounds bounds, float aboveGround) {
        BlockPos min = bounds.getMin();
        BlockPos max = bounds.getMax();

        double x = MathHelper.nextDouble(random, min.getX(), max.getX());
        double z = MathHelper.nextDouble(random, min.getZ(), max.getZ());
        double y = min.getY() + aboveGround;

        return new Vec3d(x, y, z);
    }

    public static void spawnPlayer(ServerPlayerEntity player, BlockBounds bounds, ServerWorld world) {
        Vec3d pos = SiegeSpawnLogic.choosePos(player.getRandom(), bounds, 0.5f);
        player.teleport(world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
    }
}
