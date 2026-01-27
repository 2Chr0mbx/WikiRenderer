package com.glisco.isometricrenders.render.area.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class MiniChunkScanner {
    @Nullable
    public static ChunkScanResult getConnectedChunks(Level level, BlockPos origin, int chunkSize, int blockLimit) {
        Set<MiniChunk> scannedChunks = new HashSet<>();
        Set<MiniChunk> validChunks = new HashSet<>();
        Set<MiniChunk> chunksToScan = new HashSet<>();

        Integer lowestY = null;
        Integer highestY = null;

        MiniChunk startingChunk = new MiniChunk(origin.getX(), origin.getZ(), chunkSize);
        chunksToScan.add(startingChunk);

        while (!chunksToScan.isEmpty()) {
            Set<MiniChunk> clonedChunksToScan = new HashSet<>(chunksToScan);
            chunksToScan.clear();

            for (MiniChunk chunk : clonedChunksToScan) {
                scannedChunks.add(chunk);

                if (chunk.higherAxisDistance(origin) > blockLimit) continue;

                MinYMaxY heightData = chunk.getMinYMaxY(level);
                if (heightData == null) continue;

                if (lowestY == null || heightData.minY() < lowestY) lowestY = heightData.minY();
                if (highestY == null || heightData.maxY() > highestY) highestY = heightData.maxY();

                validChunks.add(chunk);
                for (MiniChunk newChunk : chunk.createNeighbors()) {
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
