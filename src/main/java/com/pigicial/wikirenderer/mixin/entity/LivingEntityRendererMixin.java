package com.pigicial.wikirenderer.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.render.area.AreaPropertyBundle;
import com.pigicial.wikirenderer.render.entity.EntityPropertyBundle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
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
public class LivingEntityRendererMixin<T extends LivingEntity> {

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true)
    private void disablePlayerLabels(T livingEntity, double d, CallbackInfoReturnable<Boolean> cir) {
        if (WikiRenderer.inRenderableDraw && WikiRenderer.inAreaRenderDraw && AreaPropertyBundle.INSTANCE.hideNametags.get()) {
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
        if (WikiRenderer.inRenderableDraw) {
            if (WikiRenderer.inAreaRenderDraw && !AreaPropertyBundle.INSTANCE.hideNametags.get()) {
                return null;
            } else if ((WikiRenderer.inEntityDraw && !EntityPropertyBundle.INSTANCE.hideNametags.get())) {
                return null;
            }
        }
        return original.call(instance);
    }

    // layer rendering is kinda all over the place, some can be hidden by disabling models, some cant, so this is
    // a simple blacklist (maybe a whitelist is easier but ehh)
    @Unique
    private static final Set<Class<?>> BLACKLISTED_SPRITE_LAYERS = Set.of(
            ItemInHandLayer.class,
            PlayerItemInHandLayer.class,
            CarriedBlockLayer.class,
            BreezeWindLayer.class,
            CrossedArmsItemLayer.class,
            IronGolemFlowerLayer.class,
            PandaHoldsItemLayer.class,
            StuckInBodyLayer.class,
            CapeLayer.class
    );

    @Redirect(
            method = "submit*",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/EntityRenderState;FF)V"
            )
    )
    private void filterLayers(RenderLayer<EntityRenderState, EntityModel<? super EntityRenderState>> layer, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, EntityRenderState state, float yRot, float xRot) {
        if (WikiRenderer.inSpriteEntityDraw && BLACKLISTED_SPRITE_LAYERS.contains(layer.getClass())) {
            return;
        }

        layer.submit(poseStack, submitNodeCollector, light, state, yRot, xRot);
    }
}