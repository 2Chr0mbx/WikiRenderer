package com.pigicial.wikirenderer.mixin.entity.sprite;

import com.pigicial.wikirenderer.render.entity.EntitySpriteModelVisibilityUtil;
import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.renderer.entity.layers.WolfArmorLayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(WolfArmorLayer.class)
public class WolfArmorLayerMixin {

    @Shadow
    @Final
    private WolfModel adultModel;

    @Unique
    private final List<Runnable> adultModelVisibilityCallbacks = new ArrayList<>();

    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/WolfRenderState;FF)V", at = @At("TAIL"))
    private void hideNonHeadParts(CallbackInfo ci) {
        EntitySpriteModelVisibilityUtil.hideOrShowNonHeadParts(adultModel, adultModelVisibilityCallbacks);
    }
}

