package com.pigicial.wikirenderer.render.area.bounds.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class HorizontalMiniChunk {
    public final int startX;
    public final int endX;
    public final int startZ;
    public final int endZ;
    private final int size;

    public HorizontalMiniChunk(int startX, int startZ, int size) {
        this.startX = startX;
        this.size = size;
        this.endX = startX + (size - 1);
        this.startZ = startZ;
        this.endZ = startZ + (size - 1);
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

                // im not a big fan of this way of checking, but height maps were found to be unreliable (somehow)
                for (int y = maxY; y >= minY; y--) {
                    pos.setY(y);
                    if (!level.getBlockState(pos).isAir()) {
                        foundAnyBlocks = true;
                        highestY = Math.max(highestY, y);
                        int foundY = y;

                        for (y = minY; y <= foundY; y++) {
                            pos.setY(y);
                            if (!level.getBlockState(pos).isAir()) {
                                lowestY = Math.min(lowestY, y);
                                break;
                            }
                        }

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

    public List<HorizontalMiniChunk> createNeighbors() {
        return List.of(
                new HorizontalMiniChunk(startX + size, startZ, size),
                new HorizontalMiniChunk(startX, startZ + size, size),
                new HorizontalMiniChunk(startX - size, startZ, size),
                new HorizontalMiniChunk(startX, startZ - size, size)
        );
    }

    public boolean isWithinLargerMesh(int startX, int startZ, int endX, int endZ) {
        return this.startX >= startX && this.endX <= endX && this.startZ >= startZ && this.endZ <= endZ;
    }

    public boolean isBlockInThisMesh(int x, int z) {
        return x >= this.startX && x <= this.endX && z >= this.startZ && z <= this.endZ;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        HorizontalMiniChunk miniChunk = (HorizontalMiniChunk) o;
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
