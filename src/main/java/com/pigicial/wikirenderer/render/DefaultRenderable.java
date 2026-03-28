package com.pigicial.wikirenderer.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pigicial.wikirenderer.mixin.access.CameraInvoker;
import com.pigicial.wikirenderer.mixin.access.GameRendererAccessor;
import com.pigicial.wikirenderer.mixin.access.LightmapRenderStateExtractorAccessor;
import com.pigicial.wikirenderer.property.DefaultPropertyBundle;
import com.pigicial.wikirenderer.property.GlobalProperties;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

public abstract class DefaultRenderable<P extends DefaultPropertyBundle> implements Renderable<P> {

    protected static final int LIGHTING_UBO_SIZE = new Std140SizeCalculator().putVec3().putVec3().get();
    public static final Frustum ALWAYS_TRUE_PARTICLE_FRUSTUM = new Frustum(new Matrix4f(), new Matrix4f());

    protected GpuBuffer lightingBuffer;
    protected String customFileName = null;

    @Override
    public void setupLighting() {
        float rotation = (float) Math.toRadians(getProperties().getUsedRotation());

        // you might be wondering: what are these numbers from? how did I get them
        // well, I wanted to make it so entity renders would have 100% brightness on the top, 80% on the left side, and 60% brightness on the right side, regardless of
        // whichever isometric rotation you're at (45, 135, 225, 315) - to get those, I created a separate program to scan every combination of lighing positions to see which granted
        // those rotation numbers at each of the aforementioned rotations, and these were the closest numbers
        // so yes, they're basically magic numbers, but at least they get the job done, and entities now look like how they do on the minecraft wiki (which I think uses blockbench for renders)
        // yay!! (i think?)

        // is this the best way to do this? definitely not, but the previous version of this method didn't lead to the best results so i suppose this'll do
        Vector3f light0 = new Vector3f(-0.2f, 0.8f, -0.2f).rotateY((float) (Math.PI - rotation)).normalize();
        Vector3f light1 = new Vector3f(0.5f, 0.75f, -0.45f).rotateY((float) (Math.PI - rotation)).normalize();

        setupLighting(light0, light1);
    }

    protected void setupLighting(Vector3f light0, Vector3f light1) {
        if (this.lightingBuffer == null) {
            this.lightingBuffer = RenderSystem.getDevice().createBuffer(() -> "IsometricRenders Lighting UBO", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, LIGHTING_UBO_SIZE);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buf = Std140Builder.onStack(stack, LIGHTING_UBO_SIZE)
                    .putVec3(light0)
                    .putVec3(light1)
                    .get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.lightingBuffer.slice(), buf);
        }
        RenderSystem.setShaderLights(this.lightingBuffer.slice());

        if (this.usesWorldLightMap()) {
            this.updateWorldLightmap();
        }
    }

    protected boolean usesWorldLightMap() {
        return false;
    }

    protected void updateWorldLightmap() {
        // block lighting / general light map, not light direction (which is handled above)
        GameRendererAccessor gameRenderer = (GameRendererAccessor) Minecraft.getInstance().gameRenderer;
        LightmapRenderStateExtractor extractor = gameRenderer.wikirenderer$getLightmapRenderStateExtractor();

        ((LightmapRenderStateExtractorAccessor) extractor).wikirenderer$setNeedsUpdate(true);

        LightmapRenderState renderState = new LightmapRenderState();
        extractor.extract(renderState, 1.0F);

        Lightmap lightmap = gameRenderer.wikirenderer$getLightmap();
        lightmap.render(renderState);
    }

    @Override
    public void dispose() {
        if (this.lightingBuffer != null) {
            this.lightingBuffer.close();
            this.lightingBuffer = null;
        }

        if (this.usesWorldLightMap()) {
            this.updateWorldLightmap();
        }
    }

    @Override
    public void cleanUp() {
        if (this.usesWorldLightMap()) {
            this.updateWorldLightmap();
        }
    }

    @Override
    public void drawSubmittedRenderFeatures() {
        // Draw all buffers
        Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher().renderAllFeatures();
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
    }

    @Override
    public @Nullable String getCustomFileName() {
        return this.customFileName;
    }

    @Override
    public void setCustomFileName(@Nullable String fileName) {
        this.customFileName = fileName;
    }

    protected void drawParticles(Matrix4f transform, float tickDelta) {
        if (!GlobalProperties.get().tickParticles.get()) {
            return;
        }

        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(transform);

        Minecraft client = Minecraft.getInstance();
        // present in vanilla

        Camera camera = Minecraft.getInstance().getEntityRenderDispatcher().camera;
        if (camera == null) return;

        float previousYaw = camera.yRot();
        float previousPitch = camera.xRot();

        ((CameraInvoker) camera).wikirenderer$setRotation(this.getProperties().getUsedRotation() + 180, (float) this.getProperties().getUsedSlant());
        ParticlesRenderState particleBatch = new ParticlesRenderState();

        client.particleEngine.extract(
                particleBatch,
                ALWAYS_TRUE_PARTICLE_FRUSTUM,
                camera,
                tickDelta
        );

        /* create render state from camera object; (mostly) mirrors GameRenderer.updateCameraState */
        CameraRenderState cameraRenderState = CameraOrientationUtil.createRenderState(this);
        cameraRenderState.initialized = true;
        cameraRenderState.pos = camera.position();
        cameraRenderState.blockPos = camera.blockPosition();
        cameraRenderState.pos = camera.entity().getPosition(tickDelta);

        /* submit and render to vertexconsumers */
        particleBatch.submit(client.gameRenderer.getSubmitNodeStorage(), cameraRenderState);
        this.drawSubmittedRenderFeatures();
        particleBatch.reset();

        ((CameraInvoker) camera).wikirenderer$setRotation(previousYaw, previousPitch);

        modelView.popMatrix();
    }
}
