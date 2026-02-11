package com.pigicial.wikirenderer.render.area.side_view;

import com.pigicial.wikirenderer.render.area.AreaPropertyBundle;
import com.pigicial.wikirenderer.render.area.AreaRenderable;
import com.pigicial.wikirenderer.render.area.WorldBlockMesh;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

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
    private final boolean requireCeilingToShow;

    public WalkabilityFilter(WorldBlockMesh mesh, AreaRenderable renderable) {
        this(
                mesh,
                renderable.getProperties().walkableBlocksThreshold.get(),
                renderable.minFloorYLevelForOverhead.get(),
                renderable.maxFloorYLevelForOverhead.get(),
                renderable.getProperties().dontSearchForHigherFloorsThreshold.get(),
                renderable.getProperties().requireCeilingForCaveMode.get()
        );
    }

    public WalkabilityFilter(WorldBlockMesh mesh, int walkableHeightRequirement, int minFloorYLevel, int maxFloorYLevel, int dontSearchForHigherFloorsThreshold, boolean requireCeilingToShow) {
        this.mesh = mesh;
        this.walkableHeightRequirement = walkableHeightRequirement;
        this.minFloorYLevel = minFloorYLevel;
        this.maxFloorYLevel = maxFloorYLevel;
        this.dontSearchForHigherFloorsThreshold = dontSearchForHigherFloorsThreshold;
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

                    boolean isPassable = state.getCollisionShape(mesh.world, blockPos).isEmpty();
                    if (isPassable || state.is(Blocks.BARRIER)) {
                        if (passableBlocksAboveSolidBlockInARow > 0 || wasPreviousBlockSolid) {
                            passableBlocksAboveSolidBlockInARow++;
                        }

                        if (!disallowedFloor && passableBlocksAboveSolidBlockInARow >= walkableHeightRequirement) {
                            lastWalkableSolidBlockYLevel = OptionalInt.of(y - passableBlocksAboveSolidBlockInARow);
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
                        this.maxYLevelRenderMap.put(this.xzToBit(x, z), lastWalkableSolidBlockYLevel.getAsInt() + walkableHeightRequirement);
                    }
                }

            }
        }
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
