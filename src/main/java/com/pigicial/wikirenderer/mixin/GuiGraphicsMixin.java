package com.pigicial.wikirenderer.mixin;

import com.pigicial.wikirenderer.WikiRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {

    @Redirect(
            method = "renderTooltip",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIILnet/minecraft/resources/Identifier;)V"
            )
    )
    private void conditionalBackground(GuiGraphics guiGraphics, int p, int q, int k, int l, Identifier identifier) {
        if (!WikiRenderer.skipTooltipBackgroundRender) {
            TooltipRenderUtil.renderTooltipBackground(guiGraphics, p, q, k, l, identifier);
        }
    }
}
