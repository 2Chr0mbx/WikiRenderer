package com.glisco.isometricrenders.render.area.side_view.topdown_filters;

import com.glisco.isometricrenders.render.area.WorldMesh;
import net.minecraft.core.BlockPos;

// might add more filters later
public abstract class TopdownRenderingModeFilter {
    protected final WorldMesh mesh;

    protected TopdownRenderingModeFilter(WorldMesh mesh) {
        this.mesh = mesh;
    }

    protected long xzToBit(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public abstract void cacheData();

    public abstract boolean shouldRenderBlock(BlockPos pos);
}
