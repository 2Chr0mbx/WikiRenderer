package com.pigicial.wikirenderer.render.batch;

import com.pigicial.wikirenderer.property.DefaultPropertyBundle;
import com.pigicial.wikirenderer.property.PropertyBundle;
import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.screen.WikiRendererUI;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.util.Translate;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import org.joml.Matrix4fStack;

public class BatchPropertyBundle extends DefaultPropertyBundle {

    private final Renderable<?> renderable;
    private final PropertyBundle delegate;

    public BatchPropertyBundle(Renderable<?> renderable, PropertyBundle delegate) {
        this.renderable = renderable;
        this.delegate = delegate;

        // A bit ugly, but we copy all property values from the delegate and hook
        // the delegate onto our properties - this makes sure we don't always reset
        // the properties and that the mouse and keyboard controls actually affect the delegate
        if (this.delegate instanceof DefaultPropertyBundle clonedFrom) {
            this.scale.copyFrom(clonedFrom.scale);
            this.rotation.copyFrom(clonedFrom.rotation);
            this.slant.copyFrom(clonedFrom.slant);
            this.xOffset.copyFrom(clonedFrom.xOffset);
            this.yOffset.copyFrom(clonedFrom.yOffset);

            this.scale.listen(clonedFrom.scale);
            this.rotation.listen(clonedFrom.rotation);
            this.slant.listen(clonedFrom.slant);
            this.xOffset.listen(clonedFrom.xOffset);
            this.yOffset.listen(clonedFrom.yOffset);
        }
    }

    @Override
    public int getExportResolution(Renderable<?> renderable) {
        return this.delegate.getExportResolution(this.renderable);
    }

    @Override
    public void setExportResolution(Renderable<?> renderable, int exportResolution) {
        this.delegate.setExportResolution(this.renderable, exportResolution);
    }

    @Override
    public void buildMainGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        BatchRenderable<?> batchRenderable = (BatchRenderable<?>) renderable;

        this.delegate.buildMainGUIControls(batchRenderable.currentDelegate, screen, container);

        WikiRendererUI.sectionHeader(container, "batch.controls", true);
        try (WikiRendererUI.RowBuilder builder = WikiRendererUI.row(container)) {
            ButtonComponent startButton = Components.button(Translate.gui("batch.start"), (ButtonComponent button) -> {
                batchRenderable.start();
                button.active = false;
            });
            builder.row.child(startButton.horizontalSizing(Sizing.fixed(60)).margins(Insets.right(5)));
            builder.row.child(Components.button(Translate.gui("batch.reset"), (ButtonComponent button) -> {
                batchRenderable.reset();
                startButton.active = true;
            }));
        }

        WikiRendererUI.dynamicLabel(container, () -> Translate.gui(
                "batch.remaining",
                Math.max(0, batchRenderable.delegates.size() - batchRenderable.currentIndex - 1),
                batchRenderable.delegates.size()
        ));
    }

    @Override
    public void applyToViewMatrix(Renderable<?> renderable, Matrix4fStack modelViewStack) {
        this.delegate.applyToViewMatrix(this.renderable, modelViewStack);
    }

    @Override
    public void buildExportResolutionGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        WikiRendererUI.sectionHeader(container, "using_derived_resolution", true);
    }

    @Override
    public void buildFileNameGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {

    }
}
