package com.glisco.isometricrenders.render.item;

import com.glisco.isometricrenders.property.DefaultCroppablePropertyBundle;
import com.glisco.isometricrenders.render.Renderable;
import com.glisco.isometricrenders.screen.IsometricUI;
import com.glisco.isometricrenders.screen.RenderScreen;
import io.wispforest.owo.ui.container.FlowLayout;
import org.joml.Matrix4fStack;

public class ItemRenderablePropertyBundle extends DefaultCroppablePropertyBundle {

    @Override
    protected int getDefaultExportResolution() {
        return 300;
    }

    @Override
    protected boolean shouldCropByDefault() {
        return false;
    }

    @Override
    public void applyToViewMatrix(Renderable<?> renderable, Matrix4fStack modelViewStack) {
        float scale = (this.scale.get() / 100f) * 2f;
        modelViewStack.scale(scale, scale, scale);
    }

    @Override
    public void buildGuiControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        IsometricUI.sectionHeader(container, "transform_options", false);
        IsometricUI.intControl(container, scale, "scale", 10);
        IsometricUI.sectionHeader(container, "item_scale_warning_1", true);
        IsometricUI.sectionHeader(container, "item_scale_warning_2", false);
    }
}
