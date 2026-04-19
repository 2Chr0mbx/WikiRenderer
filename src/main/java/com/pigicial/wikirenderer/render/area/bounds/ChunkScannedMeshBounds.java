package com.pigicial.wikirenderer.render.area.bounds;

import com.pigicial.wikirenderer.render.area.bounds.chunk.ChunkScanResult;
import com.pigicial.wikirenderer.render.area.bounds.chunk.HorizontalMiniChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ChunkScannedMeshBounds implements MeshBounds {

    private final Set<HorizontalMiniChunk> chunksToGrabBlocksFrom;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    public ChunkScannedMeshBounds(ChunkScanResult result) {
        this(result.chunks(), result.minY(), result.maxY());
    }

    public ChunkScannedMeshBounds(Set<HorizontalMiniChunk> chunks, int minY, int maxY) {
        this.chunksToGrabBlocksFrom = chunks;
        this.minY = minY;
        this.maxY = maxY;

        assert !chunks.isEmpty();
        this.minX = chunks.stream().mapToInt(c -> c.startX).min().getAsInt();
        this.maxX = chunks.stream().mapToInt(c -> c.endX).max().getAsInt();
        this.minZ = chunks.stream().mapToInt(c -> c.startZ).min().getAsInt();
        this.maxZ = chunks.stream().mapToInt(c -> c.endZ).max().getAsInt();
    }

    @Override
    public boolean isInBounds(BlockPos pos) {
        return this.minX <= pos.getX() && this.minY <= pos.getY() && this.minZ <= pos.getZ()
               && this.maxX >= pos.getX() && this.maxY >= pos.getY() && this.maxZ >= pos.getZ();
    }

    @Override
    public int getSizeForSubMesh() {
        HorizontalMiniChunk firstChunk = chunksToGrabBlocksFrom.stream().findAny().orElseThrow();
        int chunkSize = (firstChunk.endX - firstChunk.startX) + 1;
        // the region size needs to be an interval of the mini chunk size, otherwise certain mini chunks can be missing
        while (chunkSize * 2 <= 32) {
            chunkSize *= 2;
        }
        return chunkSize;
    }

    @Override
    public AABB buildBoundingBox() {
        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    @Override
    public BlockPos getMinCorner() {
        return new BlockPos(minX, minY, minZ);
    }

    @Override
    public BlockPos getMaxCorner() {
        return new BlockPos(maxX, maxY, maxZ);
    }

    @Override
    public List<Iterable<BlockPos>> buildBlockPositionsForSubMesh(BlockPos from, BlockPos to) {
        List<Iterable<BlockPos>> miniChunkBlocksForThisMesh = new ArrayList<>();
        for (HorizontalMiniChunk chunk : this.chunksToGrabBlocksFrom) {
            if (chunk.isWithinLargerMesh(from.getX(), from.getZ(), to.getX(), to.getZ())) {
                miniChunkBlocksForThisMesh.add(BlockPos.betweenClosed(chunk.startX, from.getY(), chunk.startZ, chunk.endX, to.getY(), chunk.endZ));
            }
        }

        return miniChunkBlocksForThisMesh;
    }
}
