package com.glisco.isometricrenders.mixin;

import com.glisco.isometricrenders.IsometricRenders;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Override framebuffer used during AreaRenderable mesh render (World Mesher implementation detail)
@Mixin(ChunkSectionsToRender.class)
public class ChunkSectionsToRenderMixin {
	@WrapOperation(method = "renderGroup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;outputTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
	private RenderTarget overrideFramebuffer(ChunkSectionLayerGroup instance, Operation<RenderTarget> original) {
		if (IsometricRenders.mainTargetOverride != null) return IsometricRenders.mainTargetOverride;
		return instance.outputTarget();
	}
}
