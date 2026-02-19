package com.pigicial.wikirenderer.mixin.texture;

import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.property.GlobalProperties;
import com.pigicial.wikirenderer.render.export.ffmpeg.AnimationHandler;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

// used to speed up enchantment glints
@Mixin(TextureTransform.class)
public class TextureTransformMixin {

    @Redirect(
            method = "setupGlintTexturing",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;")
    )
    private static Object redirectGlintSpeed(OptionInstance<Double> instance) {
        if (WikiRenderer.inRenderableDraw && GlobalProperties.SPEED_UP_ENCHANTMENT_GLINTS.get()) {
            return 1.0D;
        } else {
            return instance.get();
        }
    }

    @ModifyConstant(method = "setupGlintTexturing", constant = @Constant(longValue = 110000L))
    private static long changeHorizontalModulo(long original) {
        if (WikiRenderer.inRenderableDraw && GlobalProperties.SPEED_UP_ENCHANTMENT_GLINTS.get()) {
            return 120000L; // 120,000L and 30,000L have a lowest common denominator of 120,000, whereas 110,000L and 30,000 require 330,000
        } else {
            return original;
        }
    }

    @ModifyConstant(method = "setupGlintTexturing", constant = @Constant(floatValue = 110000.0F))
    private static float changeHorizontalDivisor(float original) {
        if (WikiRenderer.inRenderableDraw && GlobalProperties.SPEED_UP_ENCHANTMENT_GLINTS.get()) {
            return 120000.0F;
        } else {
            return original;
        }
    }

    @Redirect(
            method = "setupGlintTexturing",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getMillis()J")
    )
    private static long changeGlintTiming() {
        if (WikiRenderer.inRenderableDraw && GlobalProperties.SYNC_ENCHANTMENT_GLINTS_TO_EXPORT.get()) {
            AnimationHandler animationHandler = WikiRenderer.currentAnimationHandler;
            if (animationHandler != null && !animationHandler.isFinished()) {
                int totalFrameCount = animationHandler.getAnimationFrames();
                int framesRenderedSoFar = totalFrameCount - animationHandler.getRemainingFrames();

                int frameRate = GlobalProperties.EXPORT_FRAMERATE.get();
                double secondsIntoAnimation = (double) framesRenderedSoFar / (double) frameRate;
                return (long) (secondsIntoAnimation * 1000);
            } else {
                return 0;
            }
        }
        return Util.getMillis();
    }
}
