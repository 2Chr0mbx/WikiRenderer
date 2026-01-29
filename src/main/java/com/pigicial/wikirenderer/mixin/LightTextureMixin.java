package com.pigicial.wikirenderer.mixin;

import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.render.area.AreaPropertyBundle;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightTexture.class)
public class LightTextureMixin {

    @WrapOperation(
            method = "updateLightTexture",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/attribute/EnvironmentAttributeProbe;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;F)Ljava/lang/Object;"
            )
    )
    private Object emulateDayLightColorForBlockRenders(EnvironmentAttributeProbe instance, EnvironmentAttribute<?> attribute, float f, Operation<Integer> original) {
        if (WikiRenderer.inRenderableDraw && AreaPropertyBundle.INSTANCE.emulateDaylight.get()) {
            if (attribute == EnvironmentAttributes.SKY_LIGHT_COLOR) {
                return -1;
            } else if (attribute == EnvironmentAttributes.SKY_LIGHT_FACTOR) {
                return 1.0f;
            }
        }

        return original.call(instance, attribute, f);
    }

    @WrapOperation(
            method = "updateLightTexture",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",
                    ordinal = 2
            )
    )

    private Object forceHighGamma(OptionInstance<Double> instance, Operation<Double> original) {
        if (WikiRenderer.inRenderableDraw && AreaPropertyBundle.INSTANCE.useFullBrightGamma.get()) {
            return 50D;
        }
        return original.call(instance);
    }

    @ModifyExpressionValue(
            method = "updateLightTexture",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z",
                    ordinal = 0
            )
    )
    private boolean forceHasNightVision(boolean original) {
        // If we're in an isometric render, force the 'if' check to succeed
        if (WikiRenderer.inRenderableDraw && AreaPropertyBundle.INSTANCE.useNightVision.get()) {
            return true;
        }
        return original;
    }

    @WrapOperation(
            method = "updateLightTexture",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;getNightVisionScale(Lnet/minecraft/world/entity/LivingEntity;F)F"
            )
    )
    private float forceFullNightVisionScale(LivingEntity entity, float f, Operation<Float> original) {
        // If we're in an isometric render, force the scale to 1.0 (no flickering)
        if (WikiRenderer.inRenderableDraw && AreaPropertyBundle.INSTANCE.useNightVision.get()) {
            return 1.0f;
        }
        return original.call(entity, f);
    }
}
