package com.pigicial.wikirenderer.render.area.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class MiniChunk {
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
        for (BlockPos pos : BlockPos.betweenClosed(startX, level.getMinY(), startZ, endX, level.getMaxY(), endZ)) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    public MinYMaxY getMinYMaxY(Level level) {
        int minY = level.getMinY();
        int maxY = level.getMaxY();

        int highestY = minY;
        int lowestY = maxY;

        boolean foundAnyBlocks = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = this.startX; x <= endX; x++) {
            pos.setX(x);
            for (int z = this.startZ; z <= endZ; z++) {
                pos.setZ(z);
                int blockMaxY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                highestY = Math.max(highestY, blockMaxY);

                // if it actually found something and didnt default to lowest
                for (int y = minY; y <= blockMaxY; y++) {
                    pos.setY(y);
                    if (!level.getBlockState(pos).isAir()) {
                        foundAnyBlocks = true;
                        lowestY = Math.min(lowestY, y);
                        break;
                    }
                }
            }
        }

        if (foundAnyBlocks) {
            return new MinYMaxY(lowestY, highestY);
        } else {
            return null;
        }
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

    public double higherAxisDistance(BlockPos pos) {
        return Math.max(Math.abs(pos.getX() - this.startX), Math.abs(pos.getZ() - this.startZ));
    }
}
