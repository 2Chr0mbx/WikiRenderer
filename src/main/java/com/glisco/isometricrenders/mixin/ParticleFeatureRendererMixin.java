package com.glisco.isometricrenders.mixin;

import com.glisco.isometricrenders.IsometricRenders;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.feature.ParticleFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ParticleFeatureRenderer.class)
public class ParticleFeatureRendererMixin {
    @WrapOperation(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;")
    )
    private RenderTarget overrideMainTarget(Minecraft instance, Operation<RenderTarget> original) {
        if (IsometricRenders.mainTargetOverride != null) {
            return IsometricRenders.mainTargetOverride;
        }
        return original.call(instance);
    }

    @WrapOperation(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;getParticlesTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;")
    )
    private RenderTarget overrideParticlesTarget(LevelRenderer instance, Operation<RenderTarget> original) {
        if (IsometricRenders.mainTargetOverride != null) {
            return IsometricRenders.mainTargetOverride;
        }
        return original.call(instance);
    }
}
