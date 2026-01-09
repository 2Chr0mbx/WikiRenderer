package com.glisco.isometricrenders.mixin;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.util.AreaSelectionHelper;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.LevelRenderState;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "renderLevel", at = @At("HEAD"), cancellable = true)
    public void dontRenderInScreen(GraphicsResourceAllocator allocator, DeltaTracker tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f matrix4f, Matrix4f projectionMatrix, GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        if (!IsometricRenders.skipWorldRender) return;

        IsometricRenders.skipWorldRender = false;
        ci.cancel();
    }

    @Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderBlockOutline(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lcom/mojang/blaze3d/vertex/PoseStack;ZLnet/minecraft/client/renderer/state/LevelRenderState;)V"))
    public void drawAreaSelection(GpuBufferSlice gpuBufferSlice, LevelRenderState worldRenderState, ProfilerFiller profiler, Matrix4f matrix4f, ResourceHandle<RenderTarget> handle, ResourceHandle<RenderTarget> handle2, boolean bl, ResourceHandle<RenderTarget> handle3, ResourceHandle<RenderTarget> handle4, CallbackInfo ci) {
        AreaSelectionHelper.renderSelectionBox();
    }
}
