package com.glisco.isometricrenders.mixin;

import com.glisco.isometricrenders.IsometricRenders;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.attribute.EnvironmentAttributes;
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
        if (IsometricRenders.inRenderableDraw) {
            if (attribute == EnvironmentAttributes.SKY_LIGHT_COLOR) {
                return -1;
            } else if (attribute == EnvironmentAttributes.SKY_LIGHT_FACTOR) {
                return 1.0f;
            }
        }

        return original.call(instance, attribute, f);
    }
}
