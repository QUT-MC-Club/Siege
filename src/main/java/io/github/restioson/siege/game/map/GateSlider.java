package io.github.restioson.siege.game.map;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import xyz.nucleoid.map_templates.BlockBounds;

import java.util.Arrays;

public final class GateSlider {
    private final BlockBounds bounds;
    private final int maxOffset;

    private final int height;
    private Slice[] slices;
    private Slice emptySlice;

    private int offset;

    public GateSlider(BlockBounds bounds, int maxOffset) {
        this.bounds = bounds;
        this.maxOffset = maxOffset;

        this.height = bounds.max().getY() - bounds.min().getY() + 1;
    }

    private Slice[] getSlices(ServerWorld world) {
        if (this.slices != null) {
            return this.slices;
        }

        BlockPos min = this.bounds.min();
        BlockPos max = this.bounds.max();

        int sizeX = max.getX() - min.getX() + 1;
        int sizeZ = max.getZ() - min.getZ() + 1;

        this.slices = new Slice[this.height];
        this.emptySlice = new Slice(sizeX, sizeZ);

        BlockPos.Mutable mutablePos = new BlockPos.Mutable();

        for (int y = 0; y < this.height; y++) {
            Slice slice = new Slice(sizeX, sizeZ);

            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    mutablePos.set(x + min.getX(), y + min.getY(), z + min.getZ());
                    BlockState state = world.getBlockState(mutablePos);

                    slice.set(x, z, state);
                }
            }

            this.slices[y] = slice;
        }

        return this.slices;
    }

    private Slice getSlice(ServerWorld world, int y) {
        Slice[] slices = this.getSlices(world);
        if (y < 0 || y >= slices.length) {
            return this.emptySlice;
        }
        return slices[y];
    }

    public void set(ServerWorld world, int offset) {
        offset = MathHelper.clamp(offset, 0, this.maxOffset);
        if (this.offset == offset) {
            return;
        }

        this.offset = offset;

        BlockPos min = this.bounds.min();

        for (int y = 0; y < this.height; y++) {
            Slice slice = this.getSlice(world, y - offset);
            slice.applyAt(world, min, y);
        }
    }

    public int getMaxOffset() {
        return this.maxOffset;
    }

    public void setOpen(ServerWorld world) {
        this.set(world, this.getMaxOffset());
    }

    public void setClosed(ServerWorld world) {
        this.set(world, 0);
    }

    static class Slice {
        final BlockState[] states;
        final int sizeX;
        final int sizeZ;

        Slice(int sizeX, int sizeZ) {
            this.states = new BlockState[sizeX * sizeZ];
            Arrays.fill(this.states, Blocks.AIR.getDefaultState());

            this.sizeX = sizeX;
            this.sizeZ = sizeZ;
        }

        void set(int x, int z, BlockState state) {
            this.states[this.index(x, z)] = state;
        }

        void applyAt(ServerWorld world, BlockPos min, int y) {
            BlockPos.Mutable mutablePos = new BlockPos.Mutable();

            for (int z = 0; z < this.sizeZ; z++) {
                for (int x = 0; x < this.sizeX; x++) {
                    mutablePos.set(min, x, y, z);
                    BlockState state = this.get(x, z);
                    world.setBlockState(mutablePos, state);
                }
            }
        }

        BlockState get(int x, int z) {
            return this.states[this.index(x, z)];
        }

        int index(int x, int z) {
            return x + z * this.sizeX;
        }
    }
}
