package com.pigicial.wikirenderer.render.area;

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
    private final BlockPos from, to;

    public MeshWorldOverrides(BlockAndTintGetter delegate, BlockPos from, BlockPos to) {
        this.delegate = delegate;
        this.from = from;
        this.to = to;
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
        return this.contains(pos)
                ? this.delegate.getFluidState(pos)
                : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public int getBrightness(@NonNull LightLayer type, @NonNull BlockPos pos) {
        return this.contains(pos)
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
        return this.from.getX() <= pos.getX() && this.from.getY() <= pos.getY() && this.from.getZ() <= pos.getZ()
               && this.to.getX() >= pos.getX() && this.to.getY() >= pos.getY() && this.to.getZ() >= pos.getZ();
    }
}
