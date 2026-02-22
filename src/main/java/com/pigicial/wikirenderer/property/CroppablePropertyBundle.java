package com.pigicial.wikirenderer.property;

import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.screen.WikiRendererUI;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.render.export.ImageRescaleMode;
import com.pigicial.wikirenderer.util.Translate;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import net.minecraft.client.gui.components.EditBox;

import static com.pigicial.wikirenderer.property.GlobalProperties.UNSAFE;

public interface CroppablePropertyBundle extends PropertyBundle {

    Property<Boolean> getCropProperty();

    Property<Boolean> getFFmpegCropProperty();

    Property<ImageRescaleMode> getRescaleMode();

    default boolean allowForRescaling() {
        return true;
    }

    @Override
    default void buildExportOptionGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        Property<Boolean> cropProperty = this.getCropProperty();
        Property<ImageRescaleMode> resizeModeProperty = this.getRescaleMode();

        boolean allowForRescaling = this.allowForRescaling();
        WikiRendererUI.booleanControl(container, cropProperty, allowForRescaling ? "crop_and_rescale_" + resizeModeProperty.get().name().toLowerCase() : "crop");
        cropProperty.futureListen(screen, (p, b) -> screen.guiRebuildScheduled = true);

        if (cropProperty.get() && allowForRescaling) {
            container.child(UIComponents.dropdown(Sizing.content())
                    .button(Translate.gui("rescale_vertically"), b -> {
                        resizeModeProperty.set(ImageRescaleMode.VERTICAL);
                        screen.guiRebuildScheduled = true;
                    })
                    .button(Translate.gui("rescale_horizontally"), b -> {
                        resizeModeProperty.set(ImageRescaleMode.HORIZONTAL);
                        screen.guiRebuildScheduled = true;
                    })
                    .button(Translate.gui("dont_rescale"), b -> {
                        resizeModeProperty.set(ImageRescaleMode.DISABLED);
                        screen.guiRebuildScheduled = true;
                    })
                    .closeWhenNotHovered(false)
                    .padding(Insets.of(5))
                    .surface(Surface.blur(10, 20))
            );
        }

        PropertyBundle.super.buildExportOptionGUIControls(renderable, screen, container);
    }

    default void buildRegularExportOptions(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        PropertyBundle.super.buildExportOptionGUIControls(renderable, screen, container);
    }

    @Override
    default void buildExportResolutionGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        String key = "renderer_resolution";
        if (allowForRescaling() && this.getCropProperty().get()) {
            key = "renderer_resolution_rescale_" + this.getRescaleMode().get().name().toLowerCase();
        }

        EditBox resolutionField = WikiRendererUI.labelledTextField(container, String.valueOf(renderable.getExportResolution()), key, Sizing.fixed(50));

        resolutionField.setFilter(s -> s.matches("\\d{0,5}"));
        resolutionField.setResponder(s -> {
            if (s.isBlank()) return;
            int resolution = Integer.parseInt(s);

            if ((resolution < 16 || resolution > 16384) && !UNSAFE.get()) {
                screen.exportButton.active = false;
            } else {
                renderable.getProperties().setExportResolution(renderable, resolution);
                screen.exportButton.active = true;
            }
        });
    }
}
