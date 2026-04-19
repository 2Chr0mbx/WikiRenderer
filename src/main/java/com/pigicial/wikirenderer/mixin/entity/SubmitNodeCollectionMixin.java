package com.pigicial.wikirenderer.mixin.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.util.AnimationTimingUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionMixin {

    @Inject(method = "submitModel", at = @At(value = "HEAD"))
    public <S> void wikirenderer$onSubmitModel(Model<? super S> model, S object, PoseStack poseStack, RenderType renderType, int i, int j, int k, @Nullable TextureAtlasSprite textureAtlasSprite, int l, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay, CallbackInfo ci) {
        if (WikiRenderer.animationTimingDataRequestedToFill != null && textureAtlasSprite != null) {
            AnimationTimingUtil.fillTimings(textureAtlasSprite, WikiRenderer.animationTimingDataRequestedToFill);
        }
    }

    @Inject(method = "submitModelPart", at = @At(value = "HEAD"))
    public void wikirenderer$onSubmitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int i, int j, TextureAtlasSprite textureAtlasSprite, boolean bl, boolean bl2, int k, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, int l, CallbackInfo ci) {
        if (WikiRenderer.animationTimingDataRequestedToFill != null && textureAtlasSprite != null) {
            AnimationTimingUtil.fillTimings(textureAtlasSprite, WikiRenderer.animationTimingDataRequestedToFill);
        }
    }

    @Inject(method = "submitItem", at = @At(value = "HEAD"))
    public void wikirenderer$onSubmitItem(PoseStack poseStack, ItemDisplayContext itemDisplayContext, int i, int j, int k, int[] is, List<BakedQuad> list, RenderType renderType, ItemStackRenderState.FoilType foilType, CallbackInfo ci) {
        if (WikiRenderer.animationTimingDataRequestedToFill != null && !list.isEmpty()) {
            AnimationTimingUtil.fillTimings(list, WikiRenderer.animationTimingDataRequestedToFill);
        }
    }

    @Inject(method = "submitFlame", at = @At(value = "HEAD"))
    public void wikirenderer$onSubmitFlame(PoseStack poseStack, EntityRenderState entityRenderState, Quaternionf quaternionf, CallbackInfo ci) {
        if (WikiRenderer.animationTimingDataRequestedToFill != null) {
            AtlasManager atlasManager = Minecraft.getInstance().getAtlasManager();
            TextureAtlasSprite fire0 = atlasManager.get(ModelBakery.FIRE_0);
            TextureAtlasSprite fire1 = atlasManager.get(ModelBakery.FIRE_1);
            AnimationTimingUtil.fillTimings(fire0, WikiRenderer.animationTimingDataRequestedToFill);
            AnimationTimingUtil.fillTimings(fire1, WikiRenderer.animationTimingDataRequestedToFill);
        }
    }
}
