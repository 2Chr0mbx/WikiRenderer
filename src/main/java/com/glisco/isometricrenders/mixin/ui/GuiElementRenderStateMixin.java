package com.glisco.isometricrenders.mixin.ui;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.screen.RenderScreen;
import com.glisco.isometricrenders.util.ModifiedDepthPipelineRenderState;
import io.wispforest.owo.ui.renderstate.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.state.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({
        BlitRenderState.class,
        BlurQuadElementRenderState.class,
        CircleElementRenderState.class,
        ColoredRectangleRenderState.class,
        GradientQuadElementRenderState.class,
        LineElementRenderState.class,
        RingElementRenderState.class,
        TiledBlitRenderState.class,
})
public abstract class GuiElementRenderStateMixin implements ModifiedDepthPipelineRenderState {

    @Unique
    private boolean isometric$customDepth;

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void captureDepthState(CallbackInfo ci) {
        this.isometric$customDepth = Minecraft.getInstance().screen instanceof RenderScreen && IsometricRenders.forceGuiDepthTesting;
    }

    @Override
    public boolean isometric$shouldUseDepthTesting() {
        return isometric$customDepth;
    }
}