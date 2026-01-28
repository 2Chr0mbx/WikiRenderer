package com.glisco.isometricrenders.mixin.ui;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.util.ModifiedDepthPipelineRenderState;
import com.glisco.isometricrenders.util.RenderPipelineOverrider;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.GuiTextRenderState;
import net.minecraft.client.renderer.DynamicUniforms;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.gui.render.GuiRenderer;
import org.joml.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

// note: disabling this breaks tooltip rendering, maybe more idk
@Mixin(GuiRenderer.class)
public class GuiRendererMixin {

    @WrapOperation(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
    private RenderTarget overrideRenderFramebuffer(Minecraft instance, Operation<RenderTarget> original) {
        if (IsometricRenders.mainTargetOverride != null) return IsometricRenders.mainTargetOverride;
        return original.call(instance);
    }

    // If it works, it's not stupid.
    @WrapOperation(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DynamicUniforms;writeTransform(Lorg/joml/Matrix4fc;Lorg/joml/Vector4fc;Lorg/joml/Vector3fc;Lorg/joml/Matrix4fc;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"))
    private GpuBufferSlice overrideDynamicTransforms(DynamicUniforms instance, Matrix4fc modelView, Vector4fc colorModulator, Vector3fc modelOffset, Matrix4fc textureMatrix, Operation<GpuBufferSlice> original) {
        if (IsometricRenders.inRenderableDraw)
            return original.call(instance,
                    RenderSystem.getModelViewMatrix(),
                    new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                    new Vector3f(0),
                    new Matrix4f());
        return original.call(instance, modelView, colorModulator, modelOffset, textureMatrix);
    }

    @WrapOperation(method = "draw", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setProjectionMatrix(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/ProjectionType;)V"))
    private void cancelProjectionSet(GpuBufferSlice projectionMatrixBuffer, ProjectionType projectionType, Operation<Void> original) {
        // Something else may have overridden the projection matrix by this point, restore the original one used for the renderable.
        if (IsometricRenders.inRenderableDraw)
            original.call(IsometricRenders.renderableDrawProjectionBuffer, ProjectionType.ORTHOGRAPHIC);
        else original.call(projectionMatrixBuffer, projectionType);
    }

    @WrapOperation(
            method = "addElementToMesh",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/state/GuiElementRenderState;pipeline()Lcom/mojang/blaze3d/pipeline/RenderPipeline;")
    )
    private RenderPipeline overridePipeline(GuiElementRenderState renderStateInstance, Operation<RenderPipeline> original) {
        RenderPipeline pipeline = original.call(renderStateInstance);

        if (renderStateInstance instanceof ModifiedDepthPipelineRenderState bypass && bypass.isometric$shouldUseDepthTesting()) {
            return RenderPipelineOverrider.getDepthTestingVariant(pipeline);
        }

        return pipeline;
    }

    @WrapOperation(
            method = "prepareText",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;forEachText(Ljava/util/function/Consumer;)V")
    )
    private void iris$wrapTextIteration(GuiRenderState instance, Consumer<GuiTextRenderState> originalAction, Operation<Void> original) {
        Consumer<GuiTextRenderState> wrappedAction = (guiTextRenderState) -> {
            IsometricRenders.currentlyProcessingDepthTestText = ((ModifiedDepthPipelineRenderState) (Object) guiTextRenderState).isometric$shouldUseDepthTesting();

            try {
                originalAction.accept(guiTextRenderState);
            } finally {
                IsometricRenders.currentlyProcessingDepthTestText = false;
            }
        };

        original.call(instance, wrappedAction);
    }
}