package com.pigicial.wikirenderer.mixin;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.property.GlobalProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FramerateLimitTracker.class)
public class FramerateLimitTrackerMixin {

    @Redirect(
            method = "getFramerateLimit",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/FramerateLimitTracker;getThrottleReason()Lcom/mojang/blaze3d/platform/FramerateLimitTracker$FramerateThrottleReason;")
    )
    private static FramerateLimitTracker.FramerateThrottleReason dontThrottleOnAnimations(FramerateLimitTracker instance) {
        if (WikiRenderer.currentAnimationHandler != null && !GlobalProperties.get().setAnimationFpsCap.get()) {
            return FramerateLimitTracker.FramerateThrottleReason.NONE;
        }

        return instance.getThrottleReason();
    }
}
