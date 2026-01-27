package com.glisco.isometricrenders.property;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.mixin.access.NativeImageInvoker;
import com.glisco.isometricrenders.render.Renderable;
import com.glisco.isometricrenders.render.RenderableDispatcher;
import com.glisco.isometricrenders.screen.IsometricUI;
import com.glisco.isometricrenders.screen.RenderScreen;
import com.glisco.isometricrenders.util.ImageTransferable;
import com.glisco.isometricrenders.util.Translate;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.Util;
import org.joml.Matrix4fStack;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import static com.glisco.isometricrenders.property.GlobalProperties.*;

public interface PropertyBundle {

    void buildMainGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container);

    default void buildRenderOptionGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        EditBox colorField = IsometricUI.labelledTextField(container, "#000000", "background_color", Sizing.fixed(50));
        colorField.setFilter(s -> s.matches("^#([A-Fa-f\\d]{0,6})$"));
        colorField.setValue("#" + String.format("%02X", backgroundColor >> 16) + String.format("%02X", backgroundColor >> 8 & 0xFF) + String.format("%02X", backgroundColor & 0xFF));
        colorField.moveCursorToStart(false);
        colorField.setResponder(s -> {
            if (s.substring(1).length() < 6) return;
            backgroundColor = Integer.parseInt(s.substring(1), 16);
        });
    }

    default void buildExportOptionGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        IsometricUI.booleanControl(container, saveIntoRoot, "dump_into_root");
        IsometricUI.booleanControl(container, overwriteLatest, "overwrite_latest");

        try (IsometricUI.RowBuilder builder = IsometricUI.row(container)) {
            screen.exportButton = Components.button(Translate.gui("export"), button -> screen.captureScheduled = true);
            builder.row.child(screen.exportButton);

            builder.row.child(Components.button(Translate.gui("open_folder"), button -> {
                Util.getPlatform().openFile(renderable.getExportPath().resolveOffset().toFile());
            }).margins(Insets.left(5)));
        }

        if (!GraphicsEnvironment.isHeadless()) {
            container.child(Components.button(Translate.gui("export_to_clipboard"), button -> {
                screen.notify(Translate.gui("copied_to_clipboard"));

                RenderableDispatcher.drawIntoImage(renderable, 0, renderable.getExportResolution(), renderable.shouldCrop(), null)
                        .whenComplete((image, t) -> {
                            try (image) {
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                WritableByteChannel channel = Channels.newChannel(stream);

                                ((NativeImageInvoker) (Object) image).isometric$write(channel);

                                ImageTransferable transferable = new ImageTransferable(javax.imageio.ImageIO.read(new ByteArrayInputStream(stream.toByteArray())));
                                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, transferable);
                            } catch (IOException e) {
                                IsometricRenders.LOGGER.error("mfw", e);
                            }
                        });
            }).horizontalSizing(Sizing.fixed(75)));
        }
    }

    default void buildExportResolutionGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        EditBox resolutionField = IsometricUI.labelledTextField(container, String.valueOf(renderable.getExportResolution()), "renderer_resolution", Sizing.fixed(50));
        resolutionField.setFilter(s -> s.matches("\\d{0,5}"));
        resolutionField.setResponder(s -> {
            if (s.isBlank()) return;
            int resolution = Integer.parseInt(s);

            if ((resolution < 16 || resolution > 16384) && !unsafe.get()) {
                screen.exportButton.active = false;
            } else {
                renderable.getProperties().setExportResolution(renderable, resolution);
                screen.exportButton.active = true;
            }
        });
    }

    void applyToViewMatrix(Renderable<?> renderable, Matrix4fStack modelViewStack);

    int getExportResolution(Renderable<?> renderable);

    void setExportResolution(Renderable<?> renderable, int resolution);
}
