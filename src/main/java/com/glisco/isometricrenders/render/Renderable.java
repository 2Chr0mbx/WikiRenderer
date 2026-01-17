package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.property.PropertyBundle;
import com.glisco.isometricrenders.util.ExportPathSpec;
import com.glisco.isometricrenders.util.ParticleRestriction;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

public interface Renderable<P extends PropertyBundle> {

    Renderable<PropertyBundle> EMPTY = new EmptyRenderable();

    default void prepare() {}

    default void setupLighting(Matrix4f modelViewMatrix) {}

    void emitVerticesThenDraw(Matrix4fStack modelViewStack, PoseStack matrices, MultiBufferSource vertexConsumers, float tickDelta);

    void drawSubmittedRenderFeatures();

    default void cleanUp() {}

    default void dispose() {}

    default ParticleRestriction<?> particleRestriction() {
        return ParticleRestriction.never();
    }

    P properties();

    ExportPathSpec exportPath();
}
