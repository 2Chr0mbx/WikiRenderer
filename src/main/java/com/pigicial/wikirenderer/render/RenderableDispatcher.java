package com.pigicial.wikirenderer.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.mixin.access.LightTextureAccessor;
import com.pigicial.wikirenderer.property.CroppablePropertyBundle;
import com.pigicial.wikirenderer.render.area.AreaRenderable;
import com.pigicial.wikirenderer.render.area.side_view.MinimapCalibratorData;
import com.pigicial.wikirenderer.util.ImageCropper;
import com.pigicial.wikirenderer.util.ImageRescaleMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.system.MemoryUtil;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class RenderableDispatcher {

    private static final PerspectiveProjectionMatrixBuffer PROJECTION_MATRIX_BUFFER = new PerspectiveProjectionMatrixBuffer("RenderableDispatcher");
    private static RenderTarget previewTarget = null;

    public static void drawIntoActiveFramebuffer(Renderable<?> renderable, float aspectRatio, float tickDelta, @Nullable Consumer<Matrix4fStack> transformer) {
        renderable.prepare();

        // view matrix = position/rotation/scale of camera
        // model/object matrix = position/rotation/scale of the model/object
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();

        modelViewStack.pushMatrix();
        modelViewStack.identity();
        if (transformer != null) {
            transformer.accept(modelViewStack);
        }

        renderable.getProperties().applyToViewMatrix(renderable, modelViewStack);

        Matrix4f projectionMatrix = new Matrix4f().setOrtho(-aspectRatio, aspectRatio, -1, 1, -100, 100);
        WikiRenderer.beginRenderableDraw(PROJECTION_MATRIX_BUFFER, projectionMatrix);
        WikiRenderer.setSortingMethod(projectionMatrix, modelViewStack);

        renderable.setupLighting(modelViewStack);
        renderable.emitVerticesThenDraw(
                modelViewStack,
                new PoseStack(),
                Minecraft.getInstance().renderBuffers().bufferSource(),
                tickDelta
        );
        renderable.drawSubmittedRenderFeatures();

        WikiRenderer.endRenderableDraw();
        modelViewStack.popMatrix();
        renderable.cleanUp();

        LightTexture lightTexture = Minecraft.getInstance().gameRenderer.lightTexture();
        ((LightTextureAccessor) lightTexture).wikirenderer$setUpdateLightTexture(true);
        lightTexture.updateLightTexture(1.0F);
    }

    public static CompletableFuture<NativeImage> drawIntoImage(Renderable<?> renderable, float tickDelta, int size, boolean crop, Consumer<MinimapCalibratorData> calibrationDataCallback) {
        return drawIntoImage(renderable, tickDelta, size, size, 1, crop, calibrationDataCallback);
    }

    public static CompletableFuture<NativeImage> drawIntoImage(Renderable<?> renderable, float tickDelta, int size, int targetSize, int iterations, boolean crop, Consumer<MinimapCalibratorData> calibrationDataCallback) {
        GpuTexture texture = drawIntoTexture(renderable, tickDelta, size);
        CompletableFuture<NativeImage> image = copyTextureIntoImage(texture).whenComplete((i, t) -> texture.close());

        boolean sideRendering = renderable instanceof AreaRenderable areaRenderable && areaRenderable.getProperties().perPixel90DegreeRendering.get();
        boolean exportMinimapData = calibrationDataCallback != null && sideRendering;

        if (crop) {
            // resize image to target height by regenerating it with an increased size
            image = image.thenApply(i -> {
                ImageCropper.CropData cropData = ImageCropper.getCropData(i);
                NativeImage nativeImage = ImageCropper.cropTransparent(i, cropData);

                if (exportMinimapData) {
                    MinimapCalibratorData calibrationData = MinimapCalibratorData.getCalibrationData((AreaRenderable) renderable, cropData, nativeImage);
                    calibrationDataCallback.accept(calibrationData);
                }

                return nativeImage;
            }).thenCompose(croppedImage -> {
                ImageRescaleMode rescaleMode = ((CroppablePropertyBundle) renderable.getProperties()).getRescaleMode().get();
                int axisSize = switch (rescaleMode) {
                    case VERTICAL -> croppedImage.getHeight();
                    case HORIZONTAL -> croppedImage.getWidth();
                    case DISABLED -> 0;
                };

                // todo: make this less arbitrary
                boolean smallEnoughToRescaleAgain = targetSize <= 1200 && iterations < 5; // kinda arbitary number

                if (rescaleMode == ImageRescaleMode.DISABLED
                    || axisSize == targetSize
                    || (!smallEnoughToRescaleAgain && axisSize > targetSize)
                    || (renderable instanceof AreaRenderable areaRenderable && areaRenderable.getProperties().perPixel90DegreeRendering.get())) {
                    return CompletableFuture.completedFuture(croppedImage);
                } else {
                    double multiplier = (double) size / (double) axisSize;
                    int newSize = (int) Math.ceil(targetSize * multiplier);

                    int maxTextureSize = RenderSystem.getDevice().getMaxTextureSize();
                    if (newSize > maxTextureSize) {
                        newSize = maxTextureSize;
                    }
                    return drawIntoImage(renderable, tickDelta, newSize, targetSize, iterations + 1, smallEnoughToRescaleAgain, null).thenApply(ImageCropper::cropTransparent);
                }
            });
        }

        return image;
    }

    public static GpuTexture drawIntoTexture(Renderable<?> renderable, float tickDelta, int size) {
        TextureTarget target = new TextureTarget("WikiRenderer RenderableDispatcher.drawIntoTexture Framebuffer", size, size, true);
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                Objects.requireNonNull(target.getColorTexture()),
                0,
                Objects.requireNonNull(target.getDepthTexture()),
                1.0
        );

        WikiRenderer.mainTargetOverride = target;
        RenderSystem.outputColorTextureOverride = target.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = target.getDepthTextureView();

        RenderableDispatcher.drawIntoActiveFramebuffer(renderable, 1, tickDelta, null);

        RenderSystem.outputColorTextureOverride = null;
        RenderSystem.outputDepthTextureOverride = null;
        WikiRenderer.mainTargetOverride = null;

        // Release depth attachment and FBO to save on VRAM - we only need
        // the color attachment texture to later turn into an image
        GpuTexture texture = RenderableDispatcher.cloneColorAttachment(target);
        target.destroyBuffers();
        return texture;
    }

    public static RenderTarget drawIntoDuplicateFramebuffer(Renderable<?> renderable, float tickDelta, @Nullable Consumer<Matrix4fStack> transformer) {
        Window window = Minecraft.getInstance().getWindow();
        int width = window.getWidth();
        int height = window.getHeight();

        if (previewTarget == null) {
            previewTarget = new TextureTarget("WikiRenderer RenderableDispatcher.drawIntoTexture Mirror Framebuffer", width, height, true);
        } else {
            if (previewTarget.width != width || previewTarget.height != height) {
                previewTarget.resize(width, height);
            }
        }

        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                Objects.requireNonNull(previewTarget.getColorTexture()),
                0,
                Objects.requireNonNull(previewTarget.getDepthTexture()),
                1.0
        );

        WikiRenderer.mainTargetOverride = previewTarget;
        RenderSystem.outputColorTextureOverride = previewTarget.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = previewTarget.getDepthTextureView();

        float aspectRatio = width / (float) height;
        RenderableDispatcher.drawIntoActiveFramebuffer(renderable, aspectRatio, tickDelta, transformer);

        RenderSystem.outputColorTextureOverride = null;
        RenderSystem.outputDepthTextureOverride = null;
        WikiRenderer.mainTargetOverride = null;

        return previewTarget;
    }

    /**
     * Copies the given color attachment from video
     * memory in to system memory, wrapped in a {@link NativeImage}
     *
     * @param gpuTexture The texture to copy
     * @return The created image
     */

    public static CompletableFuture<NativeImage> copyTextureIntoImage(@NotNull GpuTexture gpuTexture) {
        CompletableFuture<NativeImage> future = new CompletableFuture<>();

        final int width = gpuTexture.getWidth(0);
        final int height = gpuTexture.getHeight(0);

        // Optimized version of vanilla's ScreenshotRecorder.takeScreenshot
        // that simply copies an RGBA8 GpuTexture's contents to an RGBA NativeImage, with vertical flipping.

        // Color attachments [in vanilla] are always RGBA8, therefore != RGBA8 implies non-color attachment
        if (gpuTexture.getFormat() != TextureFormat.RGBA8)
            throw new IllegalStateException("Tried to copy non-compatible texture into image");

        GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "WikiRenderer RenderableDispatcher.copyTextureIntoImage buffer", GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST, 4L * width * height);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(gpuTexture, gpuBuffer, 0, () -> {
            try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(gpuBuffer, true, false)) {
                NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, width, height, false);

                // Skip redundant safety checks, do the memory copies directly.
                final long stride = 4L * width;
                final long srcBuf = MemoryUtil.memAddress(mappedView.data());
                final long dstBuf = nativeImage.getPointer();

                long src = srcBuf;
                long dst = dstBuf + stride * (height - 1);

                for (int y = 0; y < height; y++) {
                    MemoryUtil.memCopy(src, dst, stride);
                    src += stride;
                    dst -= stride;
                }

                future.complete(nativeImage);
            }

            gpuBuffer.close();
        }, 0);

        return future;
    }

    private static GpuTexture cloneColorAttachment(RenderTarget renderTarget) {
        GpuTexture original = renderTarget.getColorTexture();
        assert original != null;

        GpuTexture copy = RenderSystem.getDevice().createTexture(() -> "[IsometricRenders] Copy of: " + original.getLabel(),
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
                TextureFormat.RGBA8, renderTarget.width, renderTarget.height, 1, 1);

        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(original, copy, 0, 0, 0, 0, 0, renderTarget.width, renderTarget.height);

        return copy;
    }
}
