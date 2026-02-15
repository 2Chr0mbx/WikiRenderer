package com.pigicial.wikirenderer.mixin.world;

import com.pigicial.wikirenderer.render.export.CustomRenderPipelines;
import com.pigicial.wikirenderer.render.area.WorldBlockMesh;
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
        ChunkSectionLayer layer = (ChunkSectionLayer) (Object) this;

        if (layer == ChunkSectionLayer.CUTOUT && WorldBlockMesh.overrideCutoutRenderPipeline) {
            cir.setReturnValue(CustomRenderPipelines.CUTOUT_WITH_NO_TRANSPARENCY_AVERAGING);
        }
    }

}