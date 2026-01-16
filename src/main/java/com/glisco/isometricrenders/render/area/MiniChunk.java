package com.glisco.isometricrenders.render.area;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Objects;

public class MiniChunk {
    // block ranges sorted generally by how common they are in hypixel skyblock - starting at 0 would be slower since usually there's not stuff there, but ~60 is very common to have blocks
    // maybe this has no performance impact, not sure
    private static final int[][] Y_LEVEL_RANGES = {
            {60, 130},
            {0, 59},
            {131, 255}
    };

    public final int startX;
    public final int endX;
    public final int startZ;
    public final int endZ;
    private final int size;

    public MiniChunk(int startX, int startZ, int size) {
        this.startX = startX;
        this.size = size;
        this.endX = startX + (size - 1);
        this.startZ = startZ;
        this.endZ = startZ + (size - 1);
    }

    public boolean hasBlocks(Level level) {
        for (int[] yLevelRange : Y_LEVEL_RANGES) {
            int startingY = yLevelRange[0];
            int endingY = yLevelRange[1];

            for (BlockPos pos : BlockPos.betweenClosed(startX, startingY, startZ, endX, endingY, endZ)) {
                BlockState state = level.getBlockState(pos);
                if (!state.isAir()) {
                    return true;
                }
            }
        }


        return false;
    }

    public List<MiniChunk> createNeighbors() {
        return List.of(
                new MiniChunk(startX + size, startZ, size),
                new MiniChunk(startX, startZ + size, size),
                new MiniChunk(startX - size, startZ, size),
                new MiniChunk(startX, startZ - size, size)
        );
    }

    public boolean isWithin(int startX, int startZ, int endX, int endZ) {
        return this.startX >= startX && this.endX <= endX && this.startZ >= startZ && this.endZ <= endZ;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MiniChunk miniChunk = (MiniChunk) o;
        return startX == miniChunk.startX && startZ == miniChunk.startZ && size == miniChunk.size;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startX, startZ, size);
    }
}
