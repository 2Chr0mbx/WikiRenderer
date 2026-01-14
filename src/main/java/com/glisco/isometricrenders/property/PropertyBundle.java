package com.glisco.isometricrenders.property;

import com.glisco.isometricrenders.render.Renderable;
import com.glisco.isometricrenders.screen.RenderScreen;
import io.wispforest.owo.ui.container.FlowLayout;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4fStack;

public interface PropertyBundle {

    void buildGuiControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container);

    void applyToViewMatrix(Renderable<?> renderable, Matrix4fStack modelViewStack);
}
