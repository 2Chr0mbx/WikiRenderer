package com.glisco.isometricrenders.render.area;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
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

import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.StreamSupport;

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
        boolean inArea = this.from.getX() <= pos.getX() && this.from.getY() <= pos.getY() && this.from.getZ() <= pos.getZ()
                         && this.to.getX() >= pos.getX() && this.to.getY() >= pos.getY() && this.to.getZ() >= pos.getZ();

        if (!inArea) {
            return false;
        }

        if (false) {
            int heightWalkThreshold = 3;
            int airBlocksFoundInARow = 0;
            Integer lastFloorY = null;
            int ceiling = to.getY();
            boolean wasAir = false;
            boolean wasWalkable = false;

            boolean hasSeenAir = false;
            for (BlockPos blockPos : BlockPos.betweenClosed(pos.getX(), from.getY(), pos.getZ(), pos.getX(), to.getY(), pos.getZ())) {
                BlockState state = delegate.getBlockState(blockPos);
                if (state.isAir()) {
                    wasAir = true;
                    hasSeenAir = true;

                    airBlocksFoundInARow++;
                    if (airBlocksFoundInARow == heightWalkThreshold) {
                        lastFloorY = blockPos.getY() - heightWalkThreshold;
                        wasWalkable = true;
                    }
                } else {
                    airBlocksFoundInARow = 0;
                    if (wasAir) {
                        wasAir = false;
                        if (wasWalkable) {
                            ceiling = blockPos.getY();
                        }
                        wasWalkable = false;
                    }
                }
            }

            if (!hasSeenAir || pos.getY() >= ceiling || (lastFloorY != null && pos.getY() > lastFloorY + 1)) {
                return false;
            }
        }

        // check for all stone
        if (false) {
            if (StreamSupport.stream(BlockPos.betweenClosed(pos.getX(), from.getY(), pos.getZ(), pos.getX(), to.getY(), pos.getZ()).spliterator(), false)
                    .allMatch(p -> delegate.getBlockState(p).getBlock() == Blocks.STONE)) {
                return false;
            }
            // check for air below
            if (StreamSupport.stream(BlockPos.betweenClosed(pos.getX(), from.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()).spliterator(), false)
                    .anyMatch(p -> delegate.getBlockState(p).isAir())) {
                return false;
            }
        }

        /*
        // if its all stone show as air (this creates voids that look nice for caves)
        if (StreamSupport.stream(BlockPos.betweenClosed(pos.getX(), from.getY(), pos.getZ(), pos.getX(), to.getY(), pos.getZ()).spliterator(), false)
                .allMatch(p -> this.delegate.getBlockState(p).is(BlockTags.MINEABLE_WITH_PICKAXE))) {
            return false;
        }

        int highestFloorY = Minecraft.getInstance().player.getBlockY() - 1;
        OptionalInt highestAllowed = OptionalInt.empty();
        // show up to the highest non-stone-based block
        for (BlockPos blockPos : BlockPos.betweenClosed(pos.getX(), from.getY(), pos.getZ(), pos.getX(), to.getY(), pos.getZ())) {
            boolean isAir = this.delegate.getBlockState(blockPos).isAir();
            if (isAir && (blockPos.getY() <= highestFloorY || highestAllowed.isEmpty())) {
                highestAllowed = OptionalInt.of(blockPos.getY());
            }
        }

        if (highestAllowed.isPresent() && pos.getY() > highestAllowed.getAsInt()) {
            return false;
        }

        /*
        if (StreamSupport.stream(BlockPos.betweenClosed(pos.getX(), from.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()).spliterator(), false)
                .anyMatch(p -> this.delegate.getBlockState(p).isAir())) {
            return false;
        }
         */

        return true;
    }
}
