package com.pigicial.wikirenderer.render;

import com.pigicial.wikirenderer.property.CroppablePropertyBundle;
import com.pigicial.wikirenderer.property.PropertyBundle;
import com.pigicial.wikirenderer.util.ExportPathSpec;
import com.pigicial.wikirenderer.util.ParticleRestriction;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

public interface Renderable<P extends PropertyBundle> {

    Renderable<PropertyBundle> EMPTY = new EmptyRenderable();

    default void prepare() {}

    default void setupLighting(Matrix4f modelViewMatrix) {}

    void emitVerticesThenDraw(Matrix4fStack modelViewStack, PoseStack poseStack, MultiBufferSource vertexConsumers, float tickDelta);

    void drawSubmittedRenderFeatures();

    default void cleanUp() {}

    default void dispose() {}

    default ParticleRestriction<?> getParticleRestriction() {
        return ParticleRestriction.never();
    }

    P getProperties();

    ExportPathSpec getExportPath();

    @Nullable
    String getDefaultCustomFileName();

    default int getExportResolution() {
        return getProperties().getExportResolution(this);
    }

    default boolean shouldCrop() {
        if (getProperties() instanceof CroppablePropertyBundle croppablePropertyBundle) {
            return croppablePropertyBundle.getCropProperty().get();
        } else {
            return false;
        }
    }

    default boolean shouldCropForFfmpeg() {
        if (getProperties() instanceof CroppablePropertyBundle croppablePropertyBundle) {
            return croppablePropertyBundle.getFfmpegCropProperty().get();
        } else {
            return false;
        }
    }
}
