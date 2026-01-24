package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.mixin.access.LightTextureAccessor;
import com.glisco.isometricrenders.render.area.AreaRenderable;
import com.glisco.isometricrenders.render.area.side_view.MinimapCalibratorData;
import com.glisco.isometricrenders.util.ImageCropper;
import com.glisco.isometricrenders.util.RenderTargetUtils;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.system.MemoryUtil;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class RenderableDispatcher {

    private static final PerspectiveProjectionMatrixBuffer PROJECTION_MATRIX_BUFFER = new PerspectiveProjectionMatrixBuffer("RenderableDispatcher");

    /**
     * Renders the given renderable into the current framebuffer,
     * with the projection matrix adjusted to compensate for the buffer's
     * aspect ratio
     *
     * @param renderable  The renderable to draw
     * @param aspectRatio The aspect ratio of the current framebuffer
     * @param tickDelta   The tick delta to use
     */
    public static void drawIntoActiveFramebuffer(Renderable<?> renderable, float aspectRatio, float tickDelta, Consumer<Matrix4fStack> transformer) {
        renderable.prepare();

        // view matrix = position/rotation/scale of camera
        // model/object matrix = position/rotation/scale of the model/object

        // Prepare model view matrix
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();

        modelViewStack.pushMatrix();
        modelViewStack.identity();
        transformer.accept(modelViewStack);

        renderable.getProperties().applyToViewMatrix(renderable, modelViewStack);

        Matrix4f projectionMatrix = new Matrix4f().setOrtho(-aspectRatio, aspectRatio, -1, 1, -100, 100);
        IsometricRenders.beginRenderableDraw(PROJECTION_MATRIX_BUFFER, projectionMatrix);


        renderable.setupLighting(modelViewStack);
        renderable.emitVerticesThenDraw(
                modelViewStack,
                new PoseStack(),
                Minecraft.getInstance().renderBuffers().bufferSource(),
                tickDelta
        );
        renderable.drawSubmittedRenderFeatures();

        IsometricRenders.endRenderableDraw();
        modelViewStack.popMatrix();
        renderable.cleanUp();

        LightTexture lightTexture = Minecraft.getInstance().gameRenderer.lightTexture();
        ((LightTextureAccessor) lightTexture).isometric$setUpdateLightTexture(true);
        lightTexture.updateLightTexture(1.0F);
    }

    /**
     * Directly draws the given renderable into a {@link NativeImage} at the given resolution.
     * This method is essentially just a shorthand for {@code copyFramebufferIntoImage(drawIntoTexture(renderable, size))}
     *
     * @param renderable The renderable to draw
     * @param size       The resolution to render at
     * @return The created image
     */
    public static CompletableFuture<NativeImage> drawIntoImage(Renderable<?> renderable, float tickDelta, int size, boolean crop, Consumer<MinimapCalibratorData> calibrationDataCallback) {
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
            }).thenCompose(i -> {
                int height = i.getHeight();
                if (height >= size || (renderable instanceof AreaRenderable areaRenderable && areaRenderable.getProperties().perPixel90DegreeRendering.get())) {
                    // resizing would be pointless with this size, or if per pixel rendering is on dont do it
                    return CompletableFuture.completedFuture(i);
                } else {
                    double multiplier = (double) size / (double) height;
                    int newSize = (int) Math.ceil(size * multiplier);
                    return drawIntoImage(renderable, tickDelta, newSize, false, null).thenApply(ImageCropper::cropTransparent);
                }
            });
        }

        return image;
    }

    /**
     * Draws the given renderable into a new framebuffer. The FBO and depth attachment
     * are deleted afterwards to save video memory, only the color attachment remains
     *
     * @param renderable The renderable to render
     * @param size       The resolution to render aat
     * @return The color attachment
     */
    @SuppressWarnings("ConstantConditions")
    public static GpuTexture drawIntoTexture(Renderable<?> renderable, float tickDelta, int size) {
        TextureTarget target = new TextureTarget("Isometric Renders RenderableDispatcher.drawIntoTexture Framebuffer", size, size, true);
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(), 0, target.getDepthTexture(), 1.0);

        IsometricRenders.mainTargetOverride = target;
        RenderSystem.outputColorTextureOverride = target.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = target.getDepthTextureView();

        drawIntoActiveFramebuffer(renderable, 1, tickDelta, matrixStack -> {});

        RenderSystem.outputColorTextureOverride = null;
        RenderSystem.outputDepthTextureOverride = null;
        IsometricRenders.mainTargetOverride = null;
        GpuTexture texture = RenderTargetUtils.cloneColorAttachment(target);

        // Release depth attachment and FBO to save on VRAM - we only need
        // the color attachment texture to later turn into an image
        target.destroyBuffers();

        return texture;
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

        GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "Isometric Renders RenderableDispatcher.copyTextureIntoImage buffer", GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST, 4L * width * height);
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
}
