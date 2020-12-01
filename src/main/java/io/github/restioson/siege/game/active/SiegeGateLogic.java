package io.github.restioson.siege.game.active;

import io.github.restioson.siege.game.map.SiegeGate;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
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
            if (player == null || player.interactionManager.getGameMode() != GameMode.SURVIVAL) {
                continue;
            }

            if (gate.gateOpen.contains(player.getBlockPos())) {
                SiegePlayer participant = entry.getValue();
                if (participant.team == gate.flag.team) {
                    ownerTeamPresent = true;
                } else {
                    enemyTeamPresent = true;
                }
            }
        }

        boolean shouldOpen = ownerTeamPresent && !enemyTeamPresent;

        boolean moved = shouldOpen ? gate.tickOpen(world) : gate.tickClose(world);
        if (!moved) {
            return;
        }

        BlockPos pos = gate.portcullis.getMax();
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        world.playSound(null, x, y, z, SoundEvents.BLOCK_LADDER_STEP, SoundCategory.BLOCKS, 1.0f, world.random.nextFloat() * 0.25F + 0.6F);
    }
}
