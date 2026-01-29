package com.pigicial.wikirenderer.mixin.ui;

import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.util.ModifiedDepthPipelineRenderState;
import net.minecraft.client.gui.render.state.GlyphRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlyphRenderState.class)
public abstract class GlyphRenderStateMixin implements ModifiedDepthPipelineRenderState {

    @Unique
    private boolean wikirenderer$useDepthTesting;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void wikirenderer$grabContext(CallbackInfo ci) {
        this.wikirenderer$useDepthTesting = WikiRenderer.currentlyProcessingDepthTestText;
    }

    @Override
    public boolean wikirenderer$shouldUseDepthTesting() {
        return this.wikirenderer$useDepthTesting;
    }
}
