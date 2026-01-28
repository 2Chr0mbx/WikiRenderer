package com.glisco.isometricrenders.mixin.ui;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.screen.RenderScreen;
import com.glisco.isometricrenders.util.ModifiedDepthPipelineRenderState;
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
    private boolean isometric$customDepth;

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void captureDepthState(CallbackInfo ci) {
        this.isometric$customDepth = Minecraft.getInstance().screen instanceof RenderScreen && IsometricRenders.forceGuiDepthTesting;
    }

    @Override
    public boolean isometric$shouldUseDepthTesting() {
        return this.isometric$customDepth;
    }
}
