package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.mixin.access.CameraInvoker;
import com.glisco.isometricrenders.mixin.access.LightTextureAccessor;
import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.ParticlesRenderState;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

public abstract class DefaultRenderable<P extends DefaultPropertyBundle> implements Renderable<P> {

    protected static final int LIGHTING_UBO_SIZE = new Std140SizeCalculator().putVec3().putVec3().get();
    public static final Frustum ALWAYS_TRUE_PARTICLE_FRUSTUM = new Frustum(new Matrix4f(), new Matrix4f());

    protected GpuBuffer lightingBuffer;

    @Override
    public void setupLighting(Matrix4f modelViewMatrix) {
        // Apply inverse transform to lighting to keep it consistent
        Vector4f lightDirection = getLightDirection();
        Matrix4f lightTransform = new Matrix4f(modelViewMatrix);
        lightTransform.invert();
        lightDirection.mul(lightTransform);
        lightDirection.normalize(); // this line fixes inconsistent lighting with scale

        Vector3f transformedLightDirection = new Vector3f(lightDirection.x, lightDirection.y, lightDirection.z);

        // Lazily create the lighting UBO buffer when it's actually needed.
        if (this.lightingBuffer == null)
            this.lightingBuffer = RenderSystem.getDevice().createBuffer(() -> "IsometricRenders DefaultRenderable Lighting UBO", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, LIGHTING_UBO_SIZE);

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, LIGHTING_UBO_SIZE)
                    .putVec3(transformedLightDirection)
                    .putVec3(transformedLightDirection)
                    .get();

            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.lightingBuffer.slice(), byteBuffer);
        }

        RenderSystem.setShaderLights(this.lightingBuffer.slice());

        LightTexture lightTexture = Minecraft.getInstance().gameRenderer.lightTexture();
        ((LightTextureAccessor) lightTexture).isometric$setUpdateLightTexture(true);
        lightTexture.updateLightTexture(1.0F);
    }

    @Override
    public void dispose() {
        if (this.lightingBuffer != null) {
            this.lightingBuffer.close();
            this.lightingBuffer = null;
        }

        LightTexture lightTexture = Minecraft.getInstance().gameRenderer.lightTexture();
        ((LightTextureAccessor) lightTexture).isometric$setUpdateLightTexture(true);
        lightTexture.updateLightTexture(1.0F);
    }

    @Override
    public void drawSubmittedRenderFeatures() {
        // Draw all buffers
        Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher().renderAllFeatures();
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
    }

    protected void drawParticles(Matrix4f transform, float tickDelta) {
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(transform);

        Minecraft client = Minecraft.getInstance();
        // present in vanilla

        Camera camera = Minecraft.getInstance().getEntityRenderDispatcher().camera;
        float previousYaw = camera.yRot();
        float previousPitch = camera.xRot();

        ((CameraInvoker) camera).isometric$setRotation(this.properties().rotation.get() + 180 + this.properties().rotationOffset(), this.properties().slant.get());
        ParticlesRenderState particleBatch = new ParticlesRenderState();

        client.particleEngine.extract(
                particleBatch,
                ALWAYS_TRUE_PARTICLE_FRUSTUM,
                camera,
                tickDelta
        );

        /* create render state from camera object; (mostly) mirrors GameRenderer.updateCameraState */
        CameraRenderState cameraRenderState = new CameraRenderState();
        cameraRenderState.initialized = true;
        cameraRenderState.pos = camera.position();
        cameraRenderState.blockPos = camera.blockPosition();
        cameraRenderState.entityPos = camera.entity().getPosition(tickDelta);
        cameraRenderState.orientation.rotationYXZ(
                (float) Math.PI - (float) Math.toRadians(this.properties().rotation.get() + this.properties().rotationOffset),
                (float) Math.PI + (float) Math.toRadians(this.properties().slant.get()),
                (float) Math.PI);

        /* submit and render to vertexconsumers */
        particleBatch.submit(client.gameRenderer.getSubmitNodeStorage(), cameraRenderState);
        drawSubmittedRenderFeatures();
        particleBatch.reset();

        ((CameraInvoker) camera).isometric$setRotation(previousYaw, previousPitch);

        modelView.popMatrix();
    }

    protected Vector4f getLightDirection() {
        return new Vector4f(this.properties().lightAngle.get() / 90f, 0.35f, 1, 0);
    }
}
