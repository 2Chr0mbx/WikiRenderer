package com.pigicial.wikirenderer.mixin.access;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Accessor("fogRenderer")
    FogRenderer wikirenderer$getFogRenderer();

    @Accessor("lightmapRenderStateExtractor")
    LightmapRenderStateExtractor wikirenderer$getLightmapRenderStateExtractor();

    @Accessor("lightmap")
    Lightmap wikirenderer$getLightmap();
}
