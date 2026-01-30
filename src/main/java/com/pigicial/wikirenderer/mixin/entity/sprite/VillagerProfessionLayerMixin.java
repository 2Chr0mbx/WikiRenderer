package com.pigicial.wikirenderer.mixin.entity.sprite;

import com.pigicial.wikirenderer.render.entity.EntitySpriteModelVisibilityUtil;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(VillagerProfessionLayer.class)
public class VillagerProfessionLayerMixin<S extends LivingEntityRenderState, M extends EntityModel<S>> {

    @Shadow
    @Final
    private M noHatModel;

    @Shadow
    @Final
    private M noHatBabyModel;

    @Unique
    private final List<Runnable> noHatModelVisibilityCallbacks = new ArrayList<>();

    @Unique
    private final List<Runnable> noHatBabyModelVisibilityCallbacks = new ArrayList<>();

    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V", at = @At("TAIL"))
    private void hideNonHeadParts(CallbackInfo ci) {
        EntitySpriteModelVisibilityUtil.hideOrShowNonHeadParts(noHatModel, noHatModelVisibilityCallbacks);
        EntitySpriteModelVisibilityUtil.hideOrShowNonHeadParts(noHatBabyModel, noHatBabyModelVisibilityCallbacks);
    }
}
