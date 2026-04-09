package com.pigicial.wikirenderer.render.export;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.pigicial.wikirenderer.WikiRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.Function;

public class CustomRenderPipelines {
    public static final RenderPipeline CORE_TERRAIN_CUTOUT_NO_TRANSPARENCY = RenderPipeline.builder(RenderPipelines.TERRAIN_SNIPPET)
            .withLocation("pipeline/wikirenderer_terrain_cutout_no_transparency")
            .withFragmentShader(Identifier.fromNamespaceAndPath(WikiRenderer.MOD_ID, "core_terrain_no_transparency"))
            .withShaderDefine("ALPHA_CUTOUT", 0.5f)
            .build();

    // to fix leaves with transparent leaves turned off
    public static final RenderPipeline CORE_TERRAIN_SOLID_NO_TRANSPARENCY = RenderPipeline.builder(RenderPipelines.TERRAIN_SNIPPET)
            .withLocation("pipeline/wikirenderer_terrain_solid_no_transparency")
            .withFragmentShader(Identifier.fromNamespaceAndPath(WikiRenderer.MOD_ID, "core_terrain_no_transparency"))
            .build();

    // based on RenderPipelines.TEXT
    public static final RenderPipeline ITEM_FRAME_MAP_FULL_BRIGHTNESS = RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET, RenderPipelines.FOG_SNIPPET)
            .withLocation("pipeline/wikirenderer_item_frame_custom_brightness")
            .withVertexShader(Identifier.fromNamespaceAndPath(WikiRenderer.MOD_ID, "item_frame_full_bright"))
            .withFragmentShader("core/text")
            .withSampler("Sampler0")
            //.withSampler("Sampler2") normally this is included
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR, VertexFormat.Mode.QUADS)
            .build();

    private static final Function<Identifier, RenderType> ITEM_FRAME_RENDER_TYPE_CACHE = Util.memoize(
            identifier -> RenderType.create(
                    "wikirenderer_item_frame_brightness_override",
                    RenderSetup.builder(ITEM_FRAME_MAP_FULL_BRIGHTNESS).withTexture("Sampler0", identifier).bufferSize(786432).createRenderSetup()
            )
    );

    public static RenderType getCustomMapPipeline(Identifier id) {
        return ITEM_FRAME_RENDER_TYPE_CACHE.apply(id);
    }
}
