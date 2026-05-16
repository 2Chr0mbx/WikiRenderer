package com.pigicial.wikirenderer.render.area.bounds;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.List;

public interface MeshBounds {

    boolean isInBounds(BlockPos pos);

    AABB buildBoundingBox();

    BlockPos getMinCorner();

    BlockPos getMaxCorner();

    List<Iterable<BlockPos>> buildBlockPositionsForSubMesh(BlockPos from, BlockPos to);

    default String generateAreaCommand() {
        BlockPos minCorner = this.getMinCorner();
        BlockPos maxCorner = this.getMaxCorner();
        return "/wikirender area pos " + minCorner.getX() + " " + minCorner.getY() + " " + minCorner.getZ() + " " + maxCorner.getX() + " " + maxCorner.getY() + " " + maxCorner.getZ();
    }
}
