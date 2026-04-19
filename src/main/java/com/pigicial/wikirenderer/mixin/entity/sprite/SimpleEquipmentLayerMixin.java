package com.pigicial.wikirenderer.mixin.entity.sprite;

import com.pigicial.wikirenderer.render.entity.EntitySpriteModelVisibilityUtil;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(SimpleEquipmentLayer.class)
public abstract class SimpleEquipmentLayerMixin<S extends LivingEntityRenderState, EM extends EntityModel<? super S>> {

    @Shadow
    @Final
    private EM adultModel;

    @Shadow
    @Final
    @Nullable
    private EM babyModel;

    @Unique
    private final List<Runnable> adultModelVisibilityCallbacks = new ArrayList<>();

    @Unique
    private final List<Runnable> babyModelVisibilityCallbacks = new ArrayList<>();

    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V", at = @At("TAIL"))
    private void hideNonHeadParts(CallbackInfo ci) {
        EntitySpriteModelVisibilityUtil.hideOrShowNonHeadParts(adultModel, adultModelVisibilityCallbacks);
        EntitySpriteModelVisibilityUtil.hideOrShowNonHeadParts(babyModel, babyModelVisibilityCallbacks);
    }
}
