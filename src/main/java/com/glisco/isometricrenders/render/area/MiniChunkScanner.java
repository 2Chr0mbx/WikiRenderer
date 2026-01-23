package com.glisco.isometricrenders.render.area;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;

public class MiniChunkScanner {
    public static Set<MiniChunk> getConnectedChunks(Level level, BlockPos origin, int chunkSize) {
        Set<MiniChunk> scannedChunks = new HashSet<>();
        Set<MiniChunk> validChunks = new HashSet<>();

        MiniChunk startingChunk = new MiniChunk(origin.getX(), origin.getZ(), chunkSize);
        scanChunkAndAdjacentChunks(level, scannedChunks, validChunks, startingChunk);
        return validChunks;
    }

    private static void scanChunkAndAdjacentChunks(Level level, Set<MiniChunk> scannedChunks, Set<MiniChunk> validChunks, MiniChunk chunk) {
        scannedChunks.add(chunk);
        if (!chunk.hasBlocks(level)) {
            return;
        }

        validChunks.add(chunk);

        for (MiniChunk newChunk : chunk.createNeighbors()) {
            if (!scannedChunks.contains(newChunk)) {
                scanChunkAndAdjacentChunks(level, scannedChunks, validChunks, newChunk);
            }
        }
    }
}
