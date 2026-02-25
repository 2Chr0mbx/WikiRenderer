package com.pigicial.wikirenderer.render.area;

import com.pigicial.wikirenderer.render.area.bounds.MeshBounds;
import com.pigicial.wikirenderer.render.area.side_view.WalkabilityFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class MeshWorldOverrides implements BlockAndTintGetter {

    private final BlockAndTintGetter delegate;
    private final MeshBounds bounds;
    private WalkabilityFilter filter = null;

    public MeshWorldOverrides(BlockAndTintGetter delegate, MeshBounds bounds) {
        this.delegate = delegate;
        this.bounds = bounds;
    }

    @Override
    public float getShade(@NonNull Direction direction, boolean shaded) {
        return this.delegate.getShade(direction, shaded);
    }

    @Override
    @NonNull
    public LevelLightEngine getLightEngine() {
        return this.delegate.getLightEngine();
    }

    @Override
    public int getBlockTint(@NonNull BlockPos pos, @NonNull ColorResolver colorResolver) {
        return this.delegate.getBlockTint(pos, colorResolver);
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(@NonNull BlockPos pos) {
        return this.contains(pos)
                ? this.delegate.getBlockEntity(pos)
                : null;
    }

    @Override
    @NonNull
    public BlockState getBlockState(@NonNull BlockPos pos) {
        return this.contains(pos)
                ? this.delegate.getBlockState(pos)
                : Blocks.AIR.defaultBlockState();
    }

    @Override
    @NonNull
    public FluidState getFluidState(@NonNull BlockPos pos) {
        if (AreaPropertyBundle.INSTANCE.hideFluids.get()) {
            return Fluids.EMPTY.defaultFluidState();
        }
        return this.contains(pos)
                ? this.delegate.getFluidState(pos)
                : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public int getBrightness(@NonNull LightLayer type, @NonNull BlockPos pos) {
        return this.contains(pos) || !AreaPropertyBundle.INSTANCE.lightUpSurroundingBlocks.get()
                ? BlockAndTintGetter.super.getBrightness(type, pos)
                : type == LightLayer.SKY ? 15 : 0;
    }

    @Override
    public int getRawBrightness(@NonNull BlockPos pos, int ambientDarkness) {
        return this.contains(pos)
                ? BlockAndTintGetter.super.getRawBrightness(pos, ambientDarkness)
                : 15;
    }

    @Override
    public int getHeight() {
        return this.delegate.getHeight();
    }

    @Override
    public int getMinY() {
        return this.delegate.getMinY();
    }

    public boolean contains(BlockPos pos) {
        return bounds.isInBounds(pos) && (filter == null || filter.shouldRenderBlock(pos));
    }

    public void setWalkabilityFilter(WalkabilityFilter filter) {
        this.filter = filter;
    }
}
