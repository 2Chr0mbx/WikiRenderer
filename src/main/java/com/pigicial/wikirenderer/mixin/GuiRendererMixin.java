package com.pigicial.wikirenderer.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pigicial.wikirenderer.WikiRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.DynamicUniforms;
import org.joml.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// note: disabling this breaks tooltip rendering, maybe more idk
@Mixin(GuiRenderer.class)
public class GuiRendererMixin {

    @WrapOperation(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
    private RenderTarget overrideRenderFramebuffer(Minecraft instance, Operation<RenderTarget> original) {
        if (WikiRenderer.mainTargetOverride != null) return WikiRenderer.mainTargetOverride;
        return original.call(instance);
    }

    // If it works, it's not stupid.
    @WrapOperation(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DynamicUniforms;writeTransform(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"))
    private GpuBufferSlice overrideDynamicTransforms(DynamicUniforms instance, Matrix4f modelView, Operation<GpuBufferSlice> original) {
        if (WikiRenderer.inRenderableDraw) {
            return original.call(instance, RenderSystem.getModelViewMatrixCopy());
        } else {
            return original.call(instance, modelView);
        }
    }

    @WrapOperation(method = "draw", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setProjectionMatrix(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/ProjectionType;)V"))
    private void cancelProjectionSet(GpuBufferSlice projectionMatrixBuffer, ProjectionType projectionType, Operation<Void> original) {
        // Something else may have overridden the projection matrix by this point, restore the original one used for the renderable.
        if (WikiRenderer.inRenderableDraw) {
            original.call(WikiRenderer.renderableDrawProjectionBuffer, ProjectionType.ORTHOGRAPHIC);
        } else {
            original.call(projectionMatrixBuffer, projectionType);
        }
    }
}