package com.pigicial.wikirenderer.render.entity;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;

public class EntityVertexPositionTracker implements VertexConsumer {
    public static EntityVertexBounds BOUNDS = null;
    public static boolean renderingText = false;

    @Override
    public @NonNull VertexConsumer addVertex(float x, float y, float z) {
        if (BOUNDS == null) {
            BOUNDS = new EntityVertexBounds(x, y, z, renderingText);
        } else {
            BOUNDS.addPoint(x, y, z, renderingText);
        }
        return this;
    }

    @Override
    public @NonNull VertexConsumer setColor(int i, int j, int k, int l) {
        return this;
    }

    @Override
    public @NonNull VertexConsumer setColor(int i) {
        return this;
    }

    @Override
    public @NonNull VertexConsumer setUv(float f, float g) {
        return this;
    }

    @Override
    public @NonNull VertexConsumer setUv1(int i, int j) {
        return this;
    }

    @Override
    public @NonNull VertexConsumer setUv2(int i, int j) {
        return this;
    }

    @Override
    public @NonNull VertexConsumer setNormal(float f, float g, float h) {
        return this;
    }

    @Override
    public @NonNull VertexConsumer setLineWidth(float f) {
        return this;
    }

    public static class BufferSource extends MultiBufferSource.BufferSource {

        private final Map<RenderType, EntityVertexPositionTracker> renderTypeMap = new HashMap<>();
        // have to use a map to prevent duplicate buffer source issues

        public BufferSource() {
            super(null, null);
        }

        @Override
        public @NonNull VertexConsumer getBuffer(@NonNull RenderType renderType) {
            return renderTypeMap.computeIfAbsent(renderType, o -> new EntityVertexPositionTracker());
        }

        @Override
        public void endLastBatch() {

        }

        @Override
        public void endBatch() {

        }

        @Override
        public void endBatch(@NonNull RenderType renderType) {

        }
    }

    public static class OutlineBufferSource extends net.minecraft.client.renderer.OutlineBufferSource {

        private final Map<RenderType, EntityVertexPositionTracker> renderTypeMap = new HashMap<>();
        // have to use a map to prevent duplicate buffer source issues

        @Override
        public @NonNull VertexConsumer getBuffer(@NonNull RenderType renderType) {
            return renderTypeMap.computeIfAbsent(renderType, o -> new EntityVertexPositionTracker());
        }

    }
}
