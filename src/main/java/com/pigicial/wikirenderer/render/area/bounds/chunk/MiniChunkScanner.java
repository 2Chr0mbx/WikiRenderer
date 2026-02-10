package com.pigicial.wikirenderer.render.area.bounds.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class MiniChunkScanner {
    @Nullable
    public static ChunkScanResult getConnectedChunks(Level level, BlockPos origin, int chunkSize, int blockLimit) {
        Set<HorizontalMiniChunk> scannedChunks = new HashSet<>();
        Set<HorizontalMiniChunk> validChunks = new HashSet<>();
        Set<HorizontalMiniChunk> chunksToScan = new HashSet<>();

        Integer lowestY = null;
        Integer highestY = null;

        HorizontalMiniChunk startingChunk = new HorizontalMiniChunk(origin.getX(), origin.getZ(), chunkSize);
        chunksToScan.add(startingChunk);

        while (!chunksToScan.isEmpty()) {
            Set<HorizontalMiniChunk> clonedChunksToScan = new HashSet<>(chunksToScan);
            chunksToScan.clear();

            for (HorizontalMiniChunk chunk : clonedChunksToScan) {
                scannedChunks.add(chunk);

                if (chunk.higherAxisDistance(origin) > blockLimit) continue;

                MinYMaxY heightData = chunk.getMinYMaxY(level);
                if (heightData == null) continue;

                if (lowestY == null || heightData.minY() < lowestY) lowestY = heightData.minY();
                if (highestY == null || heightData.maxY() > highestY) highestY = heightData.maxY();

                validChunks.add(chunk);
                for (HorizontalMiniChunk newChunk : chunk.createNeighbors()) {
                    if (!scannedChunks.contains(newChunk)) {
                        chunksToScan.add(newChunk);
                    }
                }
            }
        }

        if (validChunks.isEmpty()) {
            return null;
        }

        return new ChunkScanResult(validChunks, lowestY, highestY);
    }
}
