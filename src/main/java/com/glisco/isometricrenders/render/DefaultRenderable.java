package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.mixin.access.CameraInvoker;
import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.mojang.blaze3d.buffers.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.ParticlesRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public abstract class DefaultRenderable<P extends DefaultPropertyBundle> implements Renderable<P> {

	private static final int LIGHTING_UBO_SIZE = new Std140SizeCalculator().putVec3().putVec3().get();
	private GpuBuffer lightingBuffer;

    @Override
    public void setupLighting(Matrix4f modelViewMatrix) {
        // Apply inverse transform to lighting to keep it consistent
        final var lightDirection = getLightDirection();
        final var lightTransform = new Matrix4f(modelViewMatrix);
        lightTransform.invert();
        lightDirection.mul(lightTransform);
		lightDirection.normalize(); // this line fixes inconsistent lighting with scale

        final var transformedLightDirection = new Vector3f(lightDirection.x, lightDirection.y, lightDirection.z);

		// Lazily create the lighting UBO buffer when it's actually needed.
		if (this.lightingBuffer == null)
			this.lightingBuffer = RenderSystem.getDevice().createBuffer(() -> "IsometricRenders DefaultRenderable Lighting UBO", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, LIGHTING_UBO_SIZE);

	    try (MemoryStack memoryStack = MemoryStack.stackPush()) {
		    ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, LIGHTING_UBO_SIZE).putVec3(transformedLightDirection).putVec3(transformedLightDirection).get();
		    RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.lightingBuffer.slice(), byteBuffer);
	    }

        RenderSystem.setShaderLights(this.lightingBuffer.slice());
    }

	@Override
	public void dispose() {
		if (this.lightingBuffer != null) {
			this.lightingBuffer.close();
			this.lightingBuffer = null;
		}
	}

	@Override
    public void draw(Matrix4f modelViewMatrix) {
        // Draw all buffers
		Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher().renderAllFeatures();
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
    }

    protected void renderParticles(Matrix4f transform, float tickDelta) {
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(transform);

        var client = Minecraft.getInstance();
        this.withParticleCamera(camera -> {
	        var particleBatch = new ParticlesRenderState();
			var pos = camera.position();
			var frustum = new Frustum(transform, IsometricRenders.renderableDrawProjectionMatrix);
	        frustum.prepare(pos.x(), pos.y(), pos.z());
			frustum.offset(-3.0F); // present in vanilla
            client.particleEngine.extract(
					particleBatch,
		            frustum,
		            camera,
		            tickDelta
            );
			/* create render state from camera object; (mostly) mirrors GameRenderer.updateCameraState */
	        var cameraRenderState = new CameraRenderState();
			cameraRenderState.initialized = true;
			cameraRenderState.pos = camera.position();
			cameraRenderState.blockPos = camera.blockPosition();
			cameraRenderState.entityPos = camera.entity().getPosition(tickDelta);
			cameraRenderState.orientation = new Quaternionf(camera.rotation());
			/* submit and render to vertexconsumers */
			particleBatch.submit(client.gameRenderer.getSubmitNodeStorage(), cameraRenderState);
	        client.gameRenderer.getFeatureRenderDispatcher().renderAllFeatures();
			particleBatch.reset();
        });

        modelView.popMatrix();
    }

    protected void withParticleCamera(Consumer<Camera> action) {
        Camera camera = Minecraft.getInstance().getEntityRenderDispatcher().camera;
        float previousYaw = camera.yRot(), previousPitch = camera.xRot();

        ((CameraInvoker) camera).isometric$setRotation(this.properties().rotation.get() + 180 + this.properties().rotationOffset(), this.properties().slant.get());
        action.accept(camera);

        ((CameraInvoker) camera).isometric$setRotation(previousYaw, previousPitch);
    }

    protected Vector4f getLightDirection() {
        return new Vector4f(this.properties().lightAngle.get() / 90f, .35f, 1, 0);
    }
}
