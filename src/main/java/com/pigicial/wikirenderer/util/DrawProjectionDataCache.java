package com.pigicial.wikirenderer.util;

import org.joml.Matrix4f;

public record DrawProjectionDataCache(Matrix4f projectionMatrix, Matrix4f modelViewStack, int width, int height) {

    public Matrix4f getModelViewProjectionMatrix() {
        return new Matrix4f(projectionMatrix).mul(modelViewStack);
    }
}
