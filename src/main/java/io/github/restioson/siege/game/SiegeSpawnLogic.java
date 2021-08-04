package io.github.restioson.siege.game;

import io.github.restioson.siege.game.map.SiegeSpawn;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;

import java.util.Random;

public class SiegeSpawnLogic {
    public static PlayerOfferResult.Accept acceptPlayer(PlayerOffer offer, ServerWorld world, SiegeSpawn spawn, GameMode gameMode) {
        var player = offer.player();
        var pos = SiegeSpawnLogic.choosePos(player.getRandom(), spawn.bounds(), 0.5F);
        return offer.accept(world, pos)
                .and(() -> {
                    player.setYaw(spawn.yaw());
                    resetPlayer(player, gameMode);
                });
    }

    public static void resetPlayer(ServerPlayerEntity player, GameMode gameMode) {
        player.changeGameMode(gameMode);
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0.0f;
        player.clearStatusEffects();
        player.setFireTicks(0);
        resetHunger(player);
    }

    private static void resetHunger(ServerPlayerEntity player) {
        NbtCompound resetTag = new NbtCompound();
        HungerManager hungerManager = new HungerManager();
        hungerManager.writeNbt(resetTag);
        player.getHungerManager().readNbt(resetTag);
    }

    public static Vec3d choosePos(Random random, BlockBounds bounds, float aboveGround) {
        BlockPos min = bounds.min();
        BlockPos max = bounds.max();

        double x = MathHelper.nextDouble(random, min.getX(), max.getX());
        double z = MathHelper.nextDouble(random, min.getZ(), max.getZ());
        double y = min.getY() + aboveGround;

        return new Vec3d(x, y, z);
    }

    public static void spawnPlayer(ServerPlayerEntity player, SiegeSpawn spawn, ServerWorld world) {
        Vec3d pos = SiegeSpawnLogic.choosePos(player.getRandom(), spawn.bounds(), 0.5f);
        player.teleport(world, pos.x, pos.y, pos.z, spawn.yaw(), 0.0F);
    }
}
