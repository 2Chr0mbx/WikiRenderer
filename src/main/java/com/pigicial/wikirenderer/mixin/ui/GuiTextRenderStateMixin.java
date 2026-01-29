package com.pigicial.wikirenderer.mixin.ui;

import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.util.ModifiedDepthPipelineRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.state.GuiTextRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiTextRenderState.class)
public abstract class GuiTextRenderStateMixin implements ModifiedDepthPipelineRenderState {

    @Unique
    private boolean wikirenderer$customDepth;

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void captureDepthState(CallbackInfo ci) {
        this.wikirenderer$customDepth = Minecraft.getInstance().screen instanceof RenderScreen && WikiRenderer.forceGuiDepthTesting;
    }

    @Override
    public boolean wikirenderer$shouldUseDepthTesting() {
        return this.wikirenderer$customDepth;
    }
}
