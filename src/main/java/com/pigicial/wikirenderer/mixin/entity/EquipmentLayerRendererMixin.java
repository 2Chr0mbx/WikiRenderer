package com.pigicial.wikirenderer.mixin.entity;

import com.pigicial.wikirenderer.WikiRenderer;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EquipmentLayerRenderer.class)
public class EquipmentLayerRendererMixin {

    @Inject(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At(value = "HEAD")
    )
    private void wikirenderer$beginArmorLayers(CallbackInfo ci, @Local(argsOnly = true) PoseStack poseStack) {
        if (WikiRenderer.inRenderableDraw) {
            poseStack.pushPose();
        }
    }

    // helps fix armor z-fighting
    @Inject(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;order(I)Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;", ordinal = 0)
    )
    private void wikirenderer$nudgeEachLayer(CallbackInfo ci, @Local(argsOnly = true) PoseStack poseStack) {
        if (WikiRenderer.inRenderableDraw) {
            poseStack.translate(0.0F, 0.75F, 0.0F);
            poseStack.scale(1.0015F, 1.0015F, 1.0015F);
            poseStack.translate(0.0F, -0.75F, 0.0F);
        }
    }

    @Inject(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At("RETURN")
    )
    private void wikirenderer$endArmorLayers(CallbackInfo ci, @Local(argsOnly = true) PoseStack poseStack) {
        if (WikiRenderer.inRenderableDraw) {
            poseStack.popPose();
        }
    }
}
