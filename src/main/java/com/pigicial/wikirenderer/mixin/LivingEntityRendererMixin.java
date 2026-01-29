package com.pigicial.wikirenderer.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.property.GlobalProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin<T extends LivingEntity> {

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true)
    private void disablePlayerLabels(T livingEntity, double d, CallbackInfoReturnable<Boolean> cir) {
        if (WikiRenderer.inRenderableDraw && GlobalProperties.HIDE_NAMETAGS.get()) {
            cir.setReturnValue(false);
        }
    }

    @WrapOperation(
            method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;getCameraEntity()Lnet/minecraft/world/entity/Entity;"
            )
    )
    private Entity overrideCameraEntity(Minecraft instance, Operation<Entity> original) {
        if (WikiRenderer.inRenderableDraw && !GlobalProperties.HIDE_NAMETAGS.get()) {
            return null;
        }
        return original.call(instance);
    }
}