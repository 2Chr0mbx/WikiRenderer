package com.pigicial.wikirenderer.render.area.bounds;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.List;

public interface MeshBounds {

    boolean isInBounds(BlockPos pos);

    int getSizeForSubMesh();

    AABB buildBoundingBox();

    BlockPos getMinCorner();

    BlockPos getMaxCorner();

    List<Iterable<BlockPos>> buildBlockPositionsForSubMesh(BlockPos from, BlockPos to);
}
