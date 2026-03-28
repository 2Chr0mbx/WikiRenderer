package com.pigicial.wikirenderer.mixin.world;

import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.property.GlobalProperties;
import com.pigicial.wikirenderer.render.ParticleDisplayCondition;
import com.pigicial.wikirenderer.screen.RenderScreen;
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
        if (!GlobalProperties.get().tickParticles.get()) {
            ci.cancel();
            return;
        }

        ParticleDisplayCondition restriction = WikiRenderer.particleDisplayCondition;
        if (!restriction.test(particle)) {
            ci.cancel();
        }
    }
}
