package com.glisco.isometricrenders.mixin;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.screen.RenderScreen;
import com.glisco.isometricrenders.util.RenderPipelineOverrider;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {

    @ModifyArg(
            method = "submitBlit",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/state/BlitRenderState;<init>(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2f;IIIIFFFFILnet/minecraft/client/gui/navigation/ScreenRectangle;)V"),
            index = 0
    )
    private RenderPipeline modifyBlitPipeline(RenderPipeline original) {
        return Minecraft.getInstance().screen instanceof RenderScreen && IsometricRenders.forceGuiDepthTesting ? RenderPipelineOverrider.getDepthTestingVariant(original) : original;
    }

    @ModifyArg(
            method = "submitColoredRectangle",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/state/ColoredRectangleRenderState;<init>(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2fc;IIIIIILnet/minecraft/client/gui/navigation/ScreenRectangle;)V"),
            index = 0
    )
    private RenderPipeline modifyColorPipeline(RenderPipeline original) {
        return Minecraft.getInstance().screen instanceof RenderScreen && IsometricRenders.forceGuiDepthTesting ? RenderPipelineOverrider.getDepthTestingVariant(original) : original;
    }

    @ModifyArg(
            method = "submitTiledBlit",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/state/TiledBlitRenderState;<init>(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2f;IIIIIIFFFFILnet/minecraft/client/gui/navigation/ScreenRectangle;)V"),
            index = 0
    )
    private RenderPipeline modifyTiledPipeline(RenderPipeline original) {
        return Minecraft.getInstance().screen instanceof RenderScreen && IsometricRenders.forceGuiDepthTesting ? RenderPipelineOverrider.getDepthTestingVariant(original) : original;
    }
}
