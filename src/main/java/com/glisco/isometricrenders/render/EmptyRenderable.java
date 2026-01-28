package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.property.PropertyBundle;
import com.glisco.isometricrenders.screen.RenderScreen;
import com.glisco.isometricrenders.util.ExportPathSpec;
import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;

public class EmptyRenderable implements Renderable<PropertyBundle> {

    private static final PropertyBundle EMPTY_BUNDLE = new PropertyBundle() {
        @Override
        public void buildMainGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {}

        @Override
        public void applyToViewMatrix(Renderable<?> renderable, Matrix4fStack modelViewStack) {}

        @Override
        public int getExportResolution(Renderable<?> renderable) {
            return 1000;
        }

        @Override
        public void setExportResolution(Renderable<?> renderable, int resolution) {}


    };

    @Override
    public void emitVerticesThenDraw(Matrix4fStack matrix4fStack, PoseStack matrices, MultiBufferSource vertexConsumers, float tickDelta) {}

    @Override
    public void drawSubmittedRenderFeatures() {}

    @Override
    public PropertyBundle getProperties() {
        return EMPTY_BUNDLE;
    }

    @Override
    public ExportPathSpec getExportPath() {
        return ExportPathSpec.of("", "empty");
    }

    @Override
    public @Nullable String getDefaultCustomFileName() {
        return null;
    }
}
