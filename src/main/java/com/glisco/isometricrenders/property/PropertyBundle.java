package com.glisco.isometricrenders.property;

import com.glisco.isometricrenders.render.Renderable;
import io.wispforest.owo.ui.container.FlowLayout;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4fStack;

public interface PropertyBundle {

    void buildGuiControls(Renderable<?> renderable, FlowLayout container);

    void applyToViewMatrix(Matrix4fStack modelViewStack);
}
