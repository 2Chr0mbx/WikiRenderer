package com.pigicial.wikirenderer.mixin.world;

import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.render.export.CustomRenderPipelines;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Objects;

@Mixin(MapRenderer.class)
public class MapRendererMixin {


    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;text(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;",
                    ordinal = 0
            )
    )
    private RenderType useMaxBrightnessMap(Identifier identifier, MapRenderState mapRenderState) {
        if (WikiRenderer.inRenderableDraw && (WikiRenderer.inSpriteEntityDraw || WikiRenderer.inAreaRenderDraw)) {
            // fixes brightness being set to 0.99824 from what is basically hardcoded in the light map (this bypasses the light map)
            return CustomRenderPipelines.getCustomMapPipeline(identifier);
        } else {
            return RenderTypes.text(Objects.requireNonNull(mapRenderState.texture));
        }
    }
}
