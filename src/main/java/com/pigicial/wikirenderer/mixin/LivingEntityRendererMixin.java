package com.pigicial.wikirenderer.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.pigicial.wikirenderer.WikiRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.pigicial.wikirenderer.property.GlobalProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.Deadmau5EarsLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

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

    @Unique
    private static final Set<Class<?>> ALLOWED_SPRITE_LAYERS = Set.of(
            HumanoidArmorLayer.class,
            Deadmau5EarsLayer.class,
            CustomHeadLayer.class
    );

    @Redirect(
            method = "submit*",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/EntityRenderState;FF)V"
            )
    )
    private void filterLayers(RenderLayer<EntityRenderState, EntityModel<? super EntityRenderState>> layer, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, EntityRenderState state, float yRot, float xRot) {
        if (!WikiRenderer.inSpriteEntityDraw || ALLOWED_SPRITE_LAYERS.contains(layer.getClass())) {
            layer.submit(poseStack, submitNodeCollector, light, state, yRot, xRot);
        }
    }
}