package com.pigicial.wikirenderer.mixin.world;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.pigicial.wikirenderer.WikiRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    // to prevent shadows from appearing on blocks that aren't in the area mesh
    @WrapOperation(method = "extractShadowPiece", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState overrideResult(ChunkAccess instance, BlockPos pos, Operation<BlockState> original) {
        if (WikiRenderer.inAreaRenderDraw && WikiRenderer.currentWorldOverrides != null) {
            return WikiRenderer.currentWorldOverrides.getBlockState(pos);
        } else {
            return original.call(instance, pos);
        }
    }
}
