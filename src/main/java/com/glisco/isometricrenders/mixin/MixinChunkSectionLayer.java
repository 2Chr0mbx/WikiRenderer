package com.glisco.isometricrenders.mixin;

import com.glisco.isometricrenders.render.area.WorldMesh;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkSectionLayer.class)
public class MixinChunkSectionLayer {


    @Inject(method = "pipeline", at = @At("HEAD"), cancellable = true)
    private void onGetPipeline(CallbackInfoReturnable<RenderPipeline> cir) {
        // 'this' refers to the ChunkSectionLayer enum instance
        ChunkSectionLayer layer = (ChunkSectionLayer) (Object) this;

        if (layer == ChunkSectionLayer.CUTOUT && WorldMesh.overrideCutoutRenderPipeline) {
            cir.setReturnValue(WorldMesh.CUTOUT_WITH_NO_TRANSPARENCY_AVERAGING);
        }
    }

}