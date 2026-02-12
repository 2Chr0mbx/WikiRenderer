package com.pigicial.wikirenderer.render.batch;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pigicial.wikirenderer.property.GlobalProperties;
import com.pigicial.wikirenderer.render.ParticleRestriction;
import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.render.export.ExportPathSpec;
import com.pigicial.wikirenderer.render.export.FileIO;
import com.pigicial.wikirenderer.screen.RenderScreen;
import org.jetbrains.annotations.Nullable;
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
    private boolean firstAnimationStarted;

    private BatchRenderable(String source, List<R> delegates) {
        this.delegates = delegates;
        this.reset(null);

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
    public void setupLighting() {
        this.currentDelegate.setupLighting();
    }

    @Override
    public void onScreenHandle(RenderScreen renderScreen) {
        GlobalProperties.popupAnimationFiles = this.batchActive;
        if (this.batchActive && this.currentIndex < this.delegates.size() && System.currentTimeMillis() - this.lastRenderTime > this.renderDelay && FileIO.taskCount() <= 5) {

            if (BatchPropertyBundle.EXPORT_AS_ANIMATIONS.get()) {
                if (renderScreen.currentAnimationExportData == null) {
                    if (!firstAnimationStarted) {
                        firstAnimationStarted = true;
                    } else {
                        this.currentDelegate.dispose();
                        this.currentIndex++;
                        this.currentDelegate = this.currentIndex < this.delegates.size() ? this.delegates.get(this.currentIndex) : this.currentDelegate;
                        this.lastRenderTime = System.currentTimeMillis();
                    }
                    renderScreen.queueAnimationExport();
                }
            } else {
                renderScreen.exportImage(false);
                this.currentDelegate.dispose();
                this.currentIndex++;
                this.currentDelegate = this.currentIndex < this.delegates.size() ? this.delegates.get(this.currentIndex) : this.currentDelegate;
                this.lastRenderTime = System.currentTimeMillis();
            }
        }
    }

    @Override
    public void emitVerticesThenDraw(RenderScreen renderScreen, Matrix4fStack matrix4fStack, PoseStack matrices, float tickDelta) {
        this.currentDelegate.emitVerticesThenDraw(renderScreen, matrix4fStack, matrices, tickDelta);
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

    protected void start() {
        this.batchActive = true;
        this.currentIndex = 0;
        this.lastRenderTime = System.currentTimeMillis();
        this.renderDelay = Math.max((int) Math.pow(this.getExportResolution() / 1024f, 2) * 100L, 75);
    }

    protected void reset(@Nullable RenderScreen screen) {
        this.batchActive = false;
        this.lastRenderTime = -1;
        this.currentIndex = -1;
        this.currentDelegate = this.delegates.getFirst();
        this.firstAnimationStarted = false;

        if (screen != null && screen.currentAnimationExportData != null) {
            screen.currentAnimationExportData.close();
            screen.currentAnimationExportData = null;
        }
    }

    protected void increaseIndex() {
        if (this.currentIndex >= this.delegates.size()) {
            this.currentIndex = 0;
        } else {
            this.currentIndex++;
        }
        this.currentDelegate = this.currentIndex < this.delegates.size() ? this.delegates.get(this.currentIndex) : this.currentDelegate;
    }

    @Override
    public BatchPropertyBundle getProperties() {
        return new BatchPropertyBundle(this.currentDelegate, this.currentDelegate.getProperties());
    }

    @Override
    public ExportPathSpec getExportPath() {
        return this.currentDelegate.getExportPath().relocate("batches/" + this.contentType);
    }

    @Override
    public @Nullable String getCustomFileName() {
        if (this.currentDelegate instanceof DynamicBatchLabelProvider provider && BatchPropertyBundle.fileNameFormatter != null) {
            return provider.buildFileName(BatchPropertyBundle.fileNameFormatter);
        } else {
            return null;
        }
    }

    @Override
    public void setCustomFileName(@Nullable String fileName) {
        BatchPropertyBundle.fileNameFormatter = fileName;
    }

}
