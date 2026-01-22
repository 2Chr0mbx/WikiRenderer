package com.glisco.isometricrenders.render.area.side_view.topdown_filters;

import com.glisco.isometricrenders.render.area.WorldMesh;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

public class CaveModeFilter extends TopdownRenderingModeFilter {
    private static final int WALKABLE_BLOCKS_HEIGHT_REQUIREMENT = 2;

    private final Map<Long, Integer> maxYLevelRenderMap = new HashMap<>();
    private final boolean requireCeilingToShow;

    public CaveModeFilter(WorldMesh mesh, boolean requireCeilingToShow) {
        super(mesh);
        this.requireCeilingToShow = requireCeilingToShow;
    }

    @Override
    public void cacheData() {
        AABB dimensions = mesh.dimensions();

        for (int x = (int) dimensions.minX; x <= dimensions.maxX; x++) {
            for (int z = (int) dimensions.minZ; z <= dimensions.maxZ; z++) {
                int passableBlocksAboveSolidBlockInARow = 0;
                boolean wasPreviousBlockSolid = false;
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

                        if (passableBlocksAboveSolidBlockInARow >= WALKABLE_BLOCKS_HEIGHT_REQUIREMENT) {
                            lastWalkableSolidBlockYLevel = OptionalInt.of(y - passableBlocksAboveSolidBlockInARow);
                        }

                        wasPreviousBlockSolid = false;
                    } else {
                        if (passableBlocksAboveSolidBlockInARow >= WALKABLE_BLOCKS_HEIGHT_REQUIREMENT) {
                            lastPreCeilingYLevel = OptionalInt.of(y - 1);
                        }
                        wasPreviousBlockSolid = true;
                        passableBlocksAboveSolidBlockInARow = 0;
                    }
                }

                if (requireCeilingToShow) {
                    if (lastPreCeilingYLevel.isPresent()) {
                        this.maxYLevelRenderMap.put(this.xzToBit(x, z), lastPreCeilingYLevel.getAsInt());
                    }
                } else {
                    if (lastWalkableSolidBlockYLevel.isPresent()) {
                        this.maxYLevelRenderMap.put(this.xzToBit(x, z), lastWalkableSolidBlockYLevel.getAsInt() + WALKABLE_BLOCKS_HEIGHT_REQUIREMENT);
                    }
                }

            }
        }
    }

    @Override
    public boolean shouldRenderBlock(BlockPos pos) {
        long bit = xzToBit(pos.getX(), pos.getZ());
        Integer maxYLevelToRender = this.maxYLevelRenderMap.get(bit);
        return maxYLevelToRender != null && pos.getY() <= maxYLevelToRender;
    }
}
