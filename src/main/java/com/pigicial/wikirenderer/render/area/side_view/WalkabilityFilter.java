package com.pigicial.wikirenderer.render.area.side_view;

import com.pigicial.wikirenderer.render.area.AreaRenderable;
import com.pigicial.wikirenderer.render.area.WorldBlockMesh;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

/**
 * A block render filter that only renders areas you can walk in, so basically a cave mode
 */
public class WalkabilityFilter {
    protected final WorldBlockMesh mesh;

    private final Map<Long, Integer> maxYLevelRenderMap = new HashMap<>();
    private final int walkableHeightRequirement;
    private final int minFloorYLevel;
    private final int maxFloorYLevel;
    private final int dontSearchForHigherFloorsThreshold;
    private final boolean includeWalls;
    private final boolean requireCeilingToShow;

    public WalkabilityFilter(WorldBlockMesh mesh, AreaRenderable renderable) {
        this(
                mesh,
                renderable.getProperties().walkableBlocksThreshold.get(),
                renderable.minFloorYLevelForOverhead.get(),
                renderable.maxFloorYLevelForOverhead.get(),
                renderable.getProperties().dontSearchForHigherFloorsThreshold.get(),
                renderable.getProperties().includeWallsForCaveMode.get(),
                renderable.getProperties().requireCeilingForCaveMode.get()
        );
    }

    public WalkabilityFilter(WorldBlockMesh mesh, int walkableHeightRequirement, int minFloorYLevel, int maxFloorYLevel, int dontSearchForHigherFloorsThreshold, boolean includeWalls, boolean requireCeilingToShow) {
        this.mesh = mesh;
        this.walkableHeightRequirement = walkableHeightRequirement;
        this.minFloorYLevel = minFloorYLevel;
        this.maxFloorYLevel = maxFloorYLevel;
        this.dontSearchForHigherFloorsThreshold = dontSearchForHigherFloorsThreshold;
        this.includeWalls = includeWalls;
        this.requireCeilingToShow = requireCeilingToShow;
    }

    public void cacheData() {
        AABB dimensions = mesh.bounds.buildBoundingBox();

        for (int x = (int) dimensions.minX; x <= dimensions.maxX; x++) {
            for (int z = (int) dimensions.minZ; z <= dimensions.maxZ; z++) {
                int passableBlocksAboveSolidBlockInARow = 0;
                boolean wasPreviousBlockSolid = false;
                boolean disallowedFloor = false;
                OptionalInt lastWalkableSolidBlockYLevel = OptionalInt.empty();
                OptionalInt lastPreCeilingYLevel = OptionalInt.empty();

                for (int y = minFloorYLevel; y <= dimensions.maxY; y++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    BlockState state = mesh.world.getBlockState(blockPos);

                    VoxelShape shape = state.getCollisionShape(mesh.world, blockPos);
                    boolean isPassable = shape.isEmpty();
                    boolean isFullFace = Block.isFaceFull(shape, Direction.DOWN) || Block.isFaceFull(shape, Direction.UP);
                    if (isPassable || !isFullFace || state.is(Blocks.BARRIER)) {
                        if (passableBlocksAboveSolidBlockInARow > 0 || wasPreviousBlockSolid) {
                            passableBlocksAboveSolidBlockInARow++;
                        }

                        if (!disallowedFloor && passableBlocksAboveSolidBlockInARow >= walkableHeightRequirement) {
                            lastWalkableSolidBlockYLevel = OptionalInt.of(y);
                        }

                        wasPreviousBlockSolid = false;
                    } else {
                        if (!disallowedFloor && passableBlocksAboveSolidBlockInARow >= walkableHeightRequirement) {
                            lastPreCeilingYLevel = OptionalInt.of(y - 1);
                        }
                        wasPreviousBlockSolid = true;
                        if (!disallowedFloor) {
                            disallowedFloor = y > this.maxFloorYLevel || passableBlocksAboveSolidBlockInARow >= dontSearchForHigherFloorsThreshold;
                        }
                        passableBlocksAboveSolidBlockInARow = 0;
                    }
                }

                if (requireCeilingToShow) {
                    if (lastPreCeilingYLevel.isPresent()) {
                        this.maxYLevelRenderMap.put(this.xzToBit(x, z), lastPreCeilingYLevel.getAsInt());
                    }
                } else {
                    if (lastWalkableSolidBlockYLevel.isPresent()) {
                        this.maxYLevelRenderMap.put(this.xzToBit(x, z), lastWalkableSolidBlockYLevel.getAsInt());
                    }
                }

            }
        }

        if (this.includeWalls) {
            this.cacheWallData(dimensions);
        }
    }

    private void cacheWallData(AABB dimensions) {
        Map<Long, Integer> modifiedMap = new HashMap<>();
        for (int x = (int) dimensions.minX; x <= dimensions.maxX; x++) {
            for (int z = (int) dimensions.minZ; z <= dimensions.maxZ; z++) {
                long bit = xzToBit(x, z);
                Integer maxYLevel = this.maxYLevelRenderMap.get(bit);
                if (maxYLevel == null) {
                    Integer highestSurroundingBlockMaxHeight = this.getHighestSurroundingBlockMaxHeight(x, z);
                    if (highestSurroundingBlockMaxHeight != null) {
                        modifiedMap.put(bit, highestSurroundingBlockMaxHeight);
                    }
                }
            }
        }

        this.maxYLevelRenderMap.putAll(modifiedMap);
    }

    private Integer getHighestSurroundingBlockMaxHeight(int x, int z) {
        Integer highest = null;
        for (int xOffset = -1; xOffset <= 1; xOffset++) {
            for (int zOffset = -1; zOffset <= 1; zOffset++) {
                Integer offsetBlockMaxHeight = this.maxYLevelRenderMap.get(xzToBit(x + xOffset, z + zOffset));
                if (offsetBlockMaxHeight != null) {
                    highest = (highest == null ? offsetBlockMaxHeight : Math.max(highest, offsetBlockMaxHeight));
                }
            }
        }
        return highest;
    }

    public boolean shouldRenderBlock(BlockPos pos) {
        long bit = xzToBit(pos.getX(), pos.getZ());
        Integer maxYLevelToRender = this.maxYLevelRenderMap.get(bit);
        return maxYLevelToRender != null && pos.getY() >= minFloorYLevel && pos.getY() <= maxYLevelToRender;
    }

    protected long xzToBit(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
