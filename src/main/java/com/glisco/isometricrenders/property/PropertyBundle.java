package com.glisco.isometricrenders.property;

import com.glisco.isometricrenders.render.Renderable;
import com.glisco.isometricrenders.screen.RenderScreen;
import io.wispforest.owo.ui.container.FlowLayout;
import org.joml.Matrix4fStack;

public interface PropertyBundle {

    void buildGuiControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container);

    void applyToViewMatrix(Renderable<?> renderable, Matrix4fStack modelViewStack);

    int getExportResolution(Renderable<?> renderable);

    void setExportResolution(int resolution);
}
