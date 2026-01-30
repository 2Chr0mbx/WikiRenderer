package com.pigicial.wikirenderer.mixin.entity.sprite;

import com.pigicial.wikirenderer.WikiRenderer;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(QuadrupedModel.class)
public class QuadrupedModelMixin {

    @Shadow
    @Final
    protected ModelPart head;
    @Shadow
    @Final
    protected ModelPart body;
    @Shadow
    @Final
    protected ModelPart rightHindLeg;
    @Shadow
    @Final
    protected ModelPart leftHindLeg;
    @Shadow
    @Final
    protected ModelPart rightFrontLeg;
    @Shadow
    @Final
    protected ModelPart leftFrontLeg;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)V", at = @At("TAIL"))
    private void hideNonHeadParts(CallbackInfo ci) {
        this.body.visible = !WikiRenderer.inSpriteEntityDraw;
        this.rightHindLeg.visible = !WikiRenderer.inSpriteEntityDraw;
        this.leftHindLeg.visible = !WikiRenderer.inSpriteEntityDraw;
        this.rightFrontLeg.visible = !WikiRenderer.inSpriteEntityDraw;
        this.leftFrontLeg.visible = !WikiRenderer.inSpriteEntityDraw;
    }
}
