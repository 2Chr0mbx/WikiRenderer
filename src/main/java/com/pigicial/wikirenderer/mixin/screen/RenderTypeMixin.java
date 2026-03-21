package com.pigicial.wikirenderer.mixin.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.pigicial.wikirenderer.WikiRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// allows item atlases to render properly during gui screen renders without having the custom model view stack data applied to it
@Mixin(RenderType.class)
public class RenderTypeMixin {
    @Inject(method = "draw", at = @At("HEAD"))
    private void wikirenderer$onDraw(MeshData meshData, CallbackInfo ci) {
        if (WikiRenderer.inContainerScreenDraw && RenderSystem.outputColorTextureOverride != null) {
            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().identity();
        }
    }

    @Inject(method = "draw", at = @At("TAIL"))
    private void wikirenderer$onDrawEnd(MeshData meshData, CallbackInfo ci) {
        if (WikiRenderer.inContainerScreenDraw && RenderSystem.outputColorTextureOverride != null) {
            RenderSystem.getModelViewStack().popMatrix();
        }
    }
}
