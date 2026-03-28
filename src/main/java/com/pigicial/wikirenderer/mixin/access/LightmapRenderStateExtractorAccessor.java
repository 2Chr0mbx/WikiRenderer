package com.pigicial.wikirenderer.mixin.access;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LightmapRenderStateExtractor.class)
public interface LightmapRenderStateExtractorAccessor {

    @Accessor("needsUpdate")
    void wikirenderer$setNeedsUpdate(boolean needsUpdate);

}
