package com.pigicial.wikirenderer.render.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.property.*;
import com.pigicial.wikirenderer.property.config.WikiRendererConfigs;
import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.screen.WikiRendererUI;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.gui.components.EditBox;
import org.joml.Matrix4fStack;

public class ContainerScreenPropertyBundle extends DefaultCroppablePropertyBundle implements SerializablePropertyBundle {

    public static final ContainerScreenPropertyBundle INSTANCE = WikiRendererConfigs.loadOrDefault(new ContainerScreenPropertyBundle());

    private final IntProperty guiScaleResolution = IntProperty.of(3, 1, 20);
    public final Property<Boolean> renderOneScaleLower = Property.of(true);

    @Override
    public boolean allowForRescaling() {
        return false;
    }

    @Override
    public String getConfigFileName() {
        return "container_screen_render_settings";
    }

    @Override
    public void buildMainGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {

    }

    @Override
    public void buildExportResolutionGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        EditBox resolutionField = WikiRendererUI.labelledTextField(container, String.valueOf(this.guiScaleResolution.get()), "gui_scale_resolution", Sizing.fixed(28));
        resolutionField.setFilter(s -> s.matches("\\d{0,3}"));
        resolutionField.setResponder(s -> {
            if (s.isBlank()) return;
            int scale = Integer.parseInt(s);

            int resolution = findValidResolutionForTargetScale(scale, this.renderOneScaleLower.get());

            if ((scale < 1 || resolution > RenderSystem.getDevice().getMaxTextureSize()) && !GlobalProperties.get().unsafe.get()) {
                screen.exportButton.active = false;
            } else {
                this.guiScaleResolution.set(scale);
                this.setExportResolution(renderable, resolution);
                screen.exportButton.active = true;
            }
        });

        WikiRendererUI.booleanControl(container, renderOneScaleLower, "render_gui_one_scale_lower");
    }

    private int findValidResolutionForTargetScale(int targetScale, boolean renderOneScaleLower) {
        for (int resolution = 0; resolution <= 9600; resolution += 80) {
            int scale = ContainerScreenRenderable.determineScaleFromResolution(resolution, resolution, renderOneScaleLower);
            if (scale == targetScale) {
                return resolution;
            }
        }

        return 0;
    }

    @Override
    public void applyToViewMatrix(Renderable<?> renderable, Matrix4fStack modelViewStack) {
        this.setExportResolution(renderable, this.findValidResolutionForTargetScale(this.guiScaleResolution.get(), this.renderOneScaleLower.get()));

        int framebufferWidth = WikiRenderer.mainTargetOverride.width;
        int framebufferHeight = WikiRenderer.mainTargetOverride.height;
        int guiScale = ContainerScreenRenderable.determineScaleFromResolution(framebufferWidth, framebufferHeight, this.renderOneScaleLower.get());

        int width = (int) (framebufferWidth / (double) guiScale);
        int screenWidth = framebufferWidth / (double) guiScale > width ? width + 1 : width;
        int height = (int) (framebufferHeight / (double) guiScale);
        int screenHeight = framebufferHeight / (double) guiScale > height ? height + 1 : height;

        double aspectRatio  = screenWidth / (double) screenHeight;
        modelViewStack.scale((float) ((2.0d * aspectRatio) / screenWidth), (float) (-2.0d / screenHeight), 1.0f);

        modelViewStack.translate(-screenWidth / 2.0f, -screenHeight / 2.0f, 0.0f);
    }
}
