package com.pigicial.wikirenderer.mixin.world;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.render.area.AreaSelectionHelper;
import com.pigicial.wikirenderer.render.area.WorldBlockMesh;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "renderLevel", at = @At("HEAD"), cancellable = true)
    public void dontRenderInScreen(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, ChunkSectionsToRender chunkSectionsToRender, CallbackInfo ci) {
        if (!WikiRenderer.skipWorldRender) return;

        WikiRenderer.skipWorldRender = false;
        ci.cancel();
    }

    @Inject(
            method = "lambda$addMainPass$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V",
                    shift = At.Shift.AFTER
            )
    )
    public void drawAreaSelection(GpuBufferSlice terrainFog, net.minecraft.client.renderer.state.level.LevelRenderState levelRenderState, ProfilerFiller profiler, ChunkSectionsToRender chunkSectionsToRender, ResourceHandle<?> entityOutlineTarget, ResourceHandle<?> translucentTarget, ResourceHandle<?> mainTarget, ResourceHandle<?> itemEntityTarget, ResourceHandle<?> particleTarget, boolean renderOutline, Matrix4fc modelViewMatrix, CallbackInfo ci) {
        AreaSelectionHelper.renderSelectionBox();
    }

    @Inject(method = "resetSampler", at = @At(value = "HEAD"))
    public void resetTerrainSampler(CallbackInfo ci) {
        if (WorldBlockMesh.terrainSampler != null) {
            WorldBlockMesh.terrainSampler.close();
            WorldBlockMesh.terrainSampler = null;
        }
    }

    @Inject(method = "getParticlesTarget", at = @At("HEAD"), cancellable = true)
    private void overrideParticlesTarget(CallbackInfoReturnable<RenderTarget> cir) {
        if (WikiRenderer.mainTargetOverride != null) {
            cir.setReturnValue(WikiRenderer.mainTargetOverride);
        }
    }
}
