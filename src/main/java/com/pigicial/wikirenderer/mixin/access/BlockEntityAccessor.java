package com.pigicial.wikirenderer.mixin.access;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockEntity.class)
public interface BlockEntityAccessor {

    @Accessor("blockState")
    void wikirenderer$setBlockState(BlockState state);

}
