package com.glisco.isometricrenders.render.item;

import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.property.IntProperty;
import com.glisco.isometricrenders.render.Renderable;
import com.glisco.isometricrenders.screen.IsometricUI;
import com.glisco.isometricrenders.screen.RenderScreen;
import com.mojang.math.Axis;
import io.wispforest.owo.ui.container.FlowLayout;
import org.joml.Matrix4fStack;

public class ItemAtlasPropertyBundle extends DefaultPropertyBundle {

    public static final ItemAtlasPropertyBundle INSTANCE = new ItemAtlasPropertyBundle();

    protected final IntProperty columns = IntProperty.of(20, 1, 500);

    @Override
    public void buildGuiControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        IsometricUI.sectionHeader(container, "transform_options", false);

        IsometricUI.intControl(container, this.scale, "scale", 10);
        IsometricUI.intControl(container, this.columns, "columns", 1);
    }

    @Override
    public void applyToViewMatrix(Renderable<?> renderable, Matrix4fStack modelViewStack) {
        super.applyToViewMatrix(renderable, modelViewStack);
        modelViewStack.rotate(Axis.YP.rotationDegrees(-this.rotation.get()));
        modelViewStack.rotate(Axis.XP.rotationDegrees(-this.slant.get().floatValue()));
    }

}
