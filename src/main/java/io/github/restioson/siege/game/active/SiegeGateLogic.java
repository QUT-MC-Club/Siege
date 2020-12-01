package io.github.restioson.siege.game.active;

import io.github.restioson.siege.game.map.SiegeGate;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import net.minecraft.block.Blocks;
import net.minecraft.item.AutomaticItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.util.PlayerRef;

public class SiegeGateLogic {
    private final SiegeActive active;

    public SiegeGateLogic(SiegeActive active) {
        this.active = active;
    }

    public void tick() {
        for (SiegeGate gate : this.active.map.gates) {
            this.tickGate(gate);
        }
    }

    public void tickGate(SiegeGate gate) {
        ServerWorld world = this.active.gameSpace.getWorld();
        boolean ownerTeamPresent = false;
        boolean enemyTeamPresent = false;

        for (Object2ObjectMap.Entry<PlayerRef, SiegePlayer> entry : Object2ObjectMaps.fastIterable(this.active.participants)) {
            ServerPlayerEntity player = entry.getKey().getEntity(world);
            if (player == null) {
                continue;
            }

            if (player.interactionManager.getGameMode() != GameMode.SURVIVAL) {
                continue;
            }

            SiegePlayer participant = entry.getValue();

            if (gate.gateOpen.contains(player.getBlockPos())) {
                if (participant.team == gate.team) {
                    ownerTeamPresent = true;
                } else {
                    enemyTeamPresent = true;
                }
            }
        }

        boolean continueMoving = ownerTeamPresent && !enemyTeamPresent;

        if (continueMoving && gate.openProgress <= gate.portcullis.getSize().getY() - 1) {
            gate.openProgress += 1;
        } else if (!continueMoving && gate.openProgress != 0) {
            gate.openProgress -= 1;
        } else {
            return;
        }

        for (BlockPos pos : gate.portcullis) {
            if (pos.getY() < gate.openProgress + gate.portcullis.getMin().getY()) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
            } else {
                AutomaticItemPlacementContext ctx = new AutomaticItemPlacementContext(world, pos, Direction.DOWN, new ItemStack(Items.OAK_FENCE), Direction.DOWN);
                world.setBlockState(pos, Blocks.OAK_FENCE.getPlacementState(ctx));
            }
        }

        BlockPos pos = gate.portcullis.getMax();
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        world.playSound(null, x, y, z, SoundEvents.BLOCK_LADDER_STEP, SoundCategory.BLOCKS, 1.0f, world.random.nextFloat() * 0.25F + 0.6F);
    }
}
