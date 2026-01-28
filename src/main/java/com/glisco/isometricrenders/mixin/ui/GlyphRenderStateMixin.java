package com.glisco.isometricrenders.mixin.ui;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.util.ModifiedDepthPipelineRenderState;
import net.minecraft.client.gui.render.state.GlyphRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlyphRenderState.class)
public abstract class GlyphRenderStateMixin implements ModifiedDepthPipelineRenderState {

    @Unique
    private boolean isometric$useDepthTesting;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void iris$grabContext(CallbackInfo ci) {
        // This is called when 'new GlyphRenderState(...)' happens inside the lambda
        this.isometric$useDepthTesting = IsometricRenders.currentlyProcessingDepthTestText;
    }

    @Override
    public boolean isometric$shouldUseDepthTesting() {
        return this.isometric$useDepthTesting;
    }
}
