package com.pigicial.wikirenderer.render.item;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.pigicial.wikirenderer.property.CroppablePropertyBundle;
import com.pigicial.wikirenderer.property.DefaultCroppablePropertyBundle;
import com.pigicial.wikirenderer.property.IntProperty;
import com.pigicial.wikirenderer.property.Property;
import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.screen.WikiRendererUI;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.gui.components.EditBox;
import org.joml.Matrix4fStack;

import static com.pigicial.wikirenderer.property.GlobalProperties.UNSAFE;

public class TooltipPropertyBundle extends DefaultCroppablePropertyBundle implements CroppablePropertyBundle {
    public static final TooltipPropertyBundle INSTANCE = new TooltipPropertyBundle();

    private final IntProperty fontScaling = IntProperty.of(4, 1, 128);
    public final Property<Boolean> hideBackground = IntProperty.of(false);

    @Override
    public boolean allowForRescaling() {
        return false;
    }

    @Override
    public void buildMainGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        WikiRendererUI.sectionHeader(container, "tooltip_options", false);
        WikiRendererUI.booleanControl(container, hideBackground, "hide_tooltip_background");
    }

    @Override
    public void buildExportResolutionGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        EditBox resolutionField = WikiRendererUI.labelledTextField(container, String.valueOf(this.fontScaling.get()), "font_resolution", Sizing.fixed(28));
        resolutionField.setFilter(s -> s.matches("\\d{0,3}"));
        resolutionField.setResponder(s -> {
            if (s.isBlank()) return;
            int resolution = Integer.parseInt(s);

            int tooltipSize = ((TooltipRenderable) renderable).getTooltipSize();
            int bufferSizeWithThisResolution = tooltipSize * resolution;

            if ((resolution < 1 || bufferSizeWithThisResolution > RenderSystem.getDevice().getMaxTextureSize()) && !UNSAFE.get()) {
                screen.exportButton.active = false;
            } else {
                this.fontScaling.set(resolution);
                screen.exportButton.active = true;
            }
        });
    }

    @Override
    public void applyToViewMatrix(Renderable<?> r, Matrix4fStack modelViewStack) {
        TooltipRenderable renderable = (TooltipRenderable) r;

        // same logic as area overhead rendering
        double imagePixelsPerFontPixel = this.fontScaling.get();
        int tooltipSize = renderable.getTooltipSize();
        double bufferSize = tooltipSize * imagePixelsPerFontPixel;

        this.setExportResolution(renderable, (int) bufferSize);

        double pixelPerfectScale = 2.0 / (double) tooltipSize;
        modelViewStack.scale((float) pixelPerfectScale, (float) pixelPerfectScale, (float) pixelPerfectScale);

        if (tooltipSize % 2 != 0) {
            // without this the image is really blurry
            modelViewStack.translate(-0.5f, -0.5f, 0);
        }

        // invisible without this
        modelViewStack.rotate(Axis.YP.rotationDegrees(180));
        modelViewStack.rotate(Axis.ZP.rotationDegrees(180));
    }
}
