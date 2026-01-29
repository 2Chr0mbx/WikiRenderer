package com.pigicial.wikirenderer.mixin;

import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.property.GlobalProperties;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.util.ParticleRestriction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Inject(method = "add(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
    public void stopParticles(Particle particle, CallbackInfo ci) {
        if (!(Minecraft.getInstance().screen instanceof RenderScreen)) return;
        if (!GlobalProperties.TICK_PARTICLES.get()) {
            ci.cancel();
            return;
        }

        ParticleRestriction<?> restriction = WikiRenderer.particleRestriction;

        if (restriction.is(ParticleRestriction.ALLOW_NEVER)) {
            return;
        }

        if (restriction.is(ParticleRestriction.ALLOW_DURING_TICK)) {
            if (!restriction.conditionFor(ParticleRestriction.ALLOW_DURING_TICK).get()) {
                ci.cancel();
            }
        } else if (restriction.is(ParticleRestriction.ALLOW_IN_AREA)) {
            if (!restriction.conditionFor(ParticleRestriction.ALLOW_IN_AREA).test(particle.getBoundingBox())) {
                ci.cancel();
            }
        }
    }
}
