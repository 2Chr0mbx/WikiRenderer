package com.pigicial.wikirenderer.mixin.access;

import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(ModelFeatureRenderer.Storage.class)
public interface ModelFeatureRendererStorageAccessor {
    @Accessor("opaqueModelSubmits")
    Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> wikirenderer$getOpaqueModelSubmits();

    @Accessor("translucentModelSubmits")
    List<SubmitNodeStorage.TranslucentModelSubmit<?>> wikirenderer$getTranslucentModelSubmits();
}
