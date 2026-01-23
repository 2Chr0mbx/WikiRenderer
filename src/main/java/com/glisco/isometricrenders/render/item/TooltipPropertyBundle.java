package com.glisco.isometricrenders.render.item;

import com.glisco.isometricrenders.property.DefaultCroppablePropertyBundle;
import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.render.Renderable;
import com.glisco.isometricrenders.screen.IsometricUI;
import com.glisco.isometricrenders.screen.RenderScreen;
import com.mojang.math.Axis;
import io.wispforest.owo.ui.container.FlowLayout;
import org.joml.Matrix4fStack;

public class TooltipPropertyBundle extends DefaultCroppablePropertyBundle {
    public static final TooltipPropertyBundle INSTANCE = new TooltipPropertyBundle();

    @Override
    protected int getDefaultExportResolution() {
        return 500;
    }

    @Override
    public void buildGuiControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        IsometricUI.sectionHeader(container, "transform_options", false);
        IsometricUI.intControl(container, this.scale, "scale", 10);
    }

    @Override
    public void applyToViewMatrix(Renderable<?> renderable, Matrix4fStack modelViewStack) {
        final float scale = this.scale.get() / 10000f;
        modelViewStack.scale(scale, scale, -scale);

        modelViewStack.translate(this.xOffset.get() / 260f, this.yOffset.get() / -260f, 0);
        modelViewStack.rotate(Axis.YP.rotationDegrees(180));
        modelViewStack.rotate(Axis.ZP.rotationDegrees(180));
    }
}
