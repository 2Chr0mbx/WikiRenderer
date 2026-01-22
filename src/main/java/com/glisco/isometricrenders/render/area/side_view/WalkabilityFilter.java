package com.glisco.isometricrenders.render.area.side_view;

import com.glisco.isometricrenders.render.area.WorldMesh;
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
    protected final WorldMesh mesh;

    private final Map<Long, Integer> maxYLevelRenderMap = new HashMap<>();
    private final int walkableHeightRequirement;
    private final boolean requireCeilingToShow;

    public WalkabilityFilter(WorldMesh mesh, int walkableHeightRequirement, boolean requireCeilingToShow) {
        this.mesh = mesh;
        this.walkableHeightRequirement = walkableHeightRequirement;
        this.requireCeilingToShow = requireCeilingToShow;
    }

    public void cacheData() {
        AABB dimensions = mesh.dimensions();

        for (int x = (int) dimensions.minX; x <= dimensions.maxX; x++) {
            for (int z = (int) dimensions.minZ; z <= dimensions.maxZ; z++) {
                int passableBlocksAboveSolidBlockInARow = 0;
                boolean wasPreviousBlockSolid = false;
                boolean lastSolidBlockWasBedrock = false; // bedrock helps filter out dwarven mines weirdness
                OptionalInt lastWalkableSolidBlockYLevel = OptionalInt.empty();
                OptionalInt lastPreCeilingYLevel = OptionalInt.empty();

                for (int y = (int) dimensions.minY; y <= dimensions.maxY; y++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    BlockState state = mesh.world.getBlockState(blockPos);

                    boolean isPassable = state.getCollisionShape(mesh.world, blockPos).isEmpty();
                    if (isPassable) { // air, fluid, buttons, etc
                        if (passableBlocksAboveSolidBlockInARow > 0 || wasPreviousBlockSolid) {
                            passableBlocksAboveSolidBlockInARow++;
                        }

                        if (!lastSolidBlockWasBedrock && passableBlocksAboveSolidBlockInARow >= walkableHeightRequirement) {
                            lastWalkableSolidBlockYLevel = OptionalInt.of(y - passableBlocksAboveSolidBlockInARow);
                        }

                        wasPreviousBlockSolid = false;
                    } else {
                        // maybe add a bedrock check
                        if (!lastSolidBlockWasBedrock && passableBlocksAboveSolidBlockInARow >= walkableHeightRequirement) {
                            lastPreCeilingYLevel = OptionalInt.of(y - 1);
                        }
                        wasPreviousBlockSolid = true;
                        passableBlocksAboveSolidBlockInARow = 0;

                        lastSolidBlockWasBedrock = state.getBlock() == Blocks.BEDROCK;
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
        return maxYLevelToRender != null && pos.getY() <= maxYLevelToRender;
    }

    protected long xzToBit(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
