package com.pigicial.wikirenderer.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pigicial.wikirenderer.property.CroppablePropertyBundle;
import com.pigicial.wikirenderer.property.PropertyBundle;
import com.pigicial.wikirenderer.render.export.ExportPathSpec;
import com.pigicial.wikirenderer.screen.RenderScreen;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;

public interface Renderable<P extends PropertyBundle> {

    Renderable<PropertyBundle> EMPTY = new EmptyRenderable();

    default void prepare() {}

    default void onScreenHandle(RenderScreen screen) {}

    default void setupLighting() {}

    default boolean usesWorldLightMap() {
        return false;
    }

    void emitVerticesThenDraw(RenderScreen renderScreen, Matrix4fStack modelViewStack, PoseStack poseStack, float tickDelta);

    void drawSubmittedRenderFeatures();

    default void cleanUp() {}

    default void dispose() {}

    default ParticleRestriction<?> getParticleRestriction() {
        return ParticleRestriction.never();
    }

    P getProperties();

    ExportPathSpec getExportPath();

    @Nullable
    String getCustomFileName();

    void setCustomFileName(@Nullable String fileName);

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

    default boolean shouldCropForFFmpeg() {
        if (getProperties() instanceof CroppablePropertyBundle croppablePropertyBundle) {
            return croppablePropertyBundle.getFFmpegCropProperty().get();
        } else {
            return false;
        }
    }
}
