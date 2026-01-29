package com.pigicial.wikirenderer.mixin;

import com.pigicial.wikirenderer.render.DefaultRenderable;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(QuadParticleGroup.class)
public class QuadParticleGroupMixin {

    // frustum math is hard, and they're kinda weird in the context of isometric / orthographic or whatever, so lets just have ALL PARTICLES WOOOOOOOOOOO
    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;pointInFrustum(DDD)Z"))
    private boolean overrideResult(Frustum instance, double x, double y, double z, Operation<Boolean> original) {
        if (instance == DefaultRenderable.ALWAYS_TRUE_PARTICLE_FRUSTUM) {
            return true;
        }
        return original.call(instance, x, y, z);
    }
}
