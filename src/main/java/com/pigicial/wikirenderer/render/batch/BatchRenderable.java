package com.pigicial.wikirenderer.render.batch;

import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.render.RenderableDispatcher;
import com.pigicial.wikirenderer.util.ExportPathSpec;
import com.pigicial.wikirenderer.util.FileIO;
import com.pigicial.wikirenderer.util.ParticleRestriction;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.List;

public class BatchRenderable<R extends Renderable<?>> implements Renderable<BatchPropertyBundle> {

    protected final List<R> delegates;
    private final String contentType;

    protected R currentDelegate;
    protected int currentIndex;

    private long renderDelay;
    private long lastRenderTime;

    private boolean batchActive;

    private BatchRenderable(String source, List<R> delegates) {
        this.delegates = delegates;
        this.reset();

        this.contentType = ExportPathSpec.exportRoot().resolve("batches/")
                .relativize(FileIO.next(ExportPathSpec.exportRoot().resolve("batches/" + source + "/"))).toString();
        this.renderDelay = Math.max((int) Math.pow(getProperties().getExportResolution(this.currentDelegate) / 1024f, 2) * 100L, 75);
    }

    public static <R extends Renderable<?>> BatchRenderable<?> of(String source, List<R> delegates) {
        if (delegates.isEmpty()) {
            return new BatchRenderable<>(source, List.of(Renderable.EMPTY));
        } else {
            return new BatchRenderable<>(source, delegates);
        }
    }

    @Override
    public int getExportResolution() {
        return this.currentDelegate.getExportResolution();
    }

    @Override
    public void prepare() {
        this.currentDelegate.prepare();
    }

    @Override
    public void setupLighting(Matrix4f modelViewMatrix) {
        this.currentDelegate.setupLighting(modelViewMatrix);
    }

    @Override
    public void emitVerticesThenDraw(Matrix4fStack matrix4fStack, PoseStack matrices, MultiBufferSource vertexConsumers, float tickDelta) {
        this.currentDelegate.emitVerticesThenDraw(matrix4fStack, matrices, vertexConsumers, tickDelta);

        if (this.batchActive && this.currentIndex < this.delegates.size() && System.currentTimeMillis() - this.lastRenderTime > this.renderDelay && FileIO.taskCount() <= 5) {
            ExportPathSpec exportPath = this.getExportPath();
            RenderableDispatcher.drawIntoImage(this.currentDelegate, 0, this.getExportResolution(), this.shouldCrop(), null)
                    .thenCompose(image -> FileIO.saveImage(image, exportPath).whenComplete((f, _t) -> image.close()));

            this.currentIndex++;
            this.currentDelegate = this.currentIndex < this.delegates.size() ? this.delegates.get(this.currentIndex) : this.currentDelegate;
            this.lastRenderTime = System.currentTimeMillis();
        }
    }

    @Override
    public void drawSubmittedRenderFeatures() {
        this.currentDelegate.drawSubmittedRenderFeatures();
    }

    @Override
    public void cleanUp() {
        this.currentDelegate.cleanUp();
    }

    @Override
    public void dispose() {
        this.delegates.forEach(Renderable::dispose);
    }

    @Override
    public ParticleRestriction<?> getParticleRestriction() {
        return this.currentDelegate.getParticleRestriction();
    }

    @Override
    public @Nullable String getDefaultCustomFileName() {
        return null;
    }

    protected void start() {
        this.batchActive = true;
        this.currentIndex = 0;
        this.lastRenderTime = System.currentTimeMillis();
        this.renderDelay = Math.max((int) Math.pow(this.getExportResolution() / 1024f, 2) * 100L, 75);
    }

    protected void reset() {
        this.batchActive = false;
        this.lastRenderTime = -1;
        this.currentIndex = -1;
        this.currentDelegate = this.delegates.getFirst();
    }

    @Override
    public BatchPropertyBundle getProperties() {
        return new BatchPropertyBundle(this.currentDelegate, this.currentDelegate.getProperties());
    }

    @Override
    public ExportPathSpec getExportPath() {
        return this.currentDelegate.getExportPath().relocate("batches/" + this.contentType);
    }

}
