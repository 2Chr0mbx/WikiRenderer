package com.pigicial.wikirenderer.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;

public class VertexPositionTracker implements VertexConsumer {
    public static AABB BOUNDS = null;

    @Override
    public @NonNull VertexConsumer addVertex(float x, float y, float z) {
        if (BOUNDS == null) {
            BOUNDS = new AABB(x, y, z, x, y, z);
        } else {
            BOUNDS = new AABB(Math.min(BOUNDS.minX, x), Math.min(BOUNDS.minY, y), Math.min(BOUNDS.minZ, z), Math.max(BOUNDS.maxX, x), Math.max(BOUNDS.maxY, y), Math.max(BOUNDS.maxZ, z));
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

        private final Map<RenderType, VertexPositionTracker> renderTypeMap = new HashMap<>(); // have to use a map to prevent duplicate buffer source issues

        public BufferSource() {
            super(null, null);
        }

        @Override
        public @NonNull VertexConsumer getBuffer(@NonNull RenderType renderType) {
            return renderTypeMap.computeIfAbsent(renderType, o -> new VertexPositionTracker());
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

        private final Map<RenderType, VertexPositionTracker> renderTypeMap = new HashMap<>();
        // have to use a map to prevent duplicate buffer source issues

        @Override
        public @NonNull VertexConsumer getBuffer(@NonNull RenderType renderType) {
            return renderTypeMap.computeIfAbsent(renderType, o -> new VertexPositionTracker());
        }

    }
}
