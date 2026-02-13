package com.pigicial.wikirenderer.render.area.bounds;

import com.pigicial.wikirenderer.render.area.side_view.ExpansionSide;
import com.pigicial.wikirenderer.render.area.side_view.MeshSideRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class SingleCuboidMeshBounds implements ExpandableMeshBounds {
    private BlockPos min;
    private BlockPos max;

    public SingleCuboidMeshBounds(BlockPos first, BlockPos second) {
        this.min = first;
        this.max = second;
        this.recalculateCorners();
    }

    @Override
    public boolean isInBounds(BlockPos pos) {
        return this.min.getX() <= pos.getX() && this.min.getY() <= pos.getY() && this.min.getZ() <= pos.getZ()
               && this.max.getX() >= pos.getX() && this.max.getY() >= pos.getY() && this.max.getZ() >= pos.getZ();
    }

    @Override
    public int getSizeForSubMesh() {
        return 64;
    }

    @Override
    public AABB buildBoundingBox() {
        return AABB.encapsulatingFullBlocks(min, max);
    }

    @Override
    public BlockPos getMinCorner() {
        return min;
    }

    @Override
    public BlockPos getMaxCorner() {
        return max;
    }

    @Override
    public List<Iterable<BlockPos>> buildBlockPositionsForSubMesh(BlockPos from, BlockPos to) {
        return List.of(BlockPos.betweenClosed(from, to));
    }

    @Override
    public void move(ExpansionSide side, MeshSideRotation currentRotation, int multiplier) {
        this.modifyBounds(side.getDirection(currentRotation), multiplier);
    }

    private void modifyBounds(Vec3i worldFacing, int multiplier) {
        boolean isTouchingMinX = worldFacing.getX() < 0;
        boolean isTouchingMaxX = worldFacing.getX() > 0;
        boolean isTouchingMinZ = worldFacing.getZ() < 0;
        boolean isTouchingMaxZ = worldFacing.getZ() > 0;

        int minChangeX = isTouchingMinX ? (worldFacing.getX() * multiplier) : 0;
        int maxChangeX = isTouchingMaxX ? (worldFacing.getX() * multiplier) : 0;
        int minChangeZ = isTouchingMinZ ? (worldFacing.getZ() * multiplier) : 0;
        int maxChangeZ = isTouchingMaxZ ? (worldFacing.getZ() * multiplier) : 0;

        this.min = min.offset(minChangeX, 0, minChangeZ);
        this.max = max.offset(maxChangeX, 0, maxChangeZ);
        this.recalculateCorners();
    }

    // theres probably a better way to do this but whatever
    private void recalculateCorners() {
        int minX = Math.min(min.getX(), max.getX());
        int maxX = Math.max(min.getX(), max.getX());
        int minY = Math.min(min.getY(), max.getY());
        int maxY = Math.max(min.getY(), max.getY());
        int minZ = Math.min(min.getZ(), max.getZ());
        int maxZ = Math.max(min.getZ(), max.getZ());

        this.min = new BlockPos(minX, minY, minZ);
        this.max = new BlockPos(maxX, maxY, maxZ);
    }
}
