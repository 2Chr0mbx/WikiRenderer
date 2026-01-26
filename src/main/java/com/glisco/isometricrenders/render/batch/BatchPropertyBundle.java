package com.glisco.isometricrenders.render.batch;

import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.property.PropertyBundle;
import com.glisco.isometricrenders.render.Renderable;
import com.glisco.isometricrenders.screen.IsometricUI;
import com.glisco.isometricrenders.screen.RenderScreen;
import com.glisco.isometricrenders.util.Translate;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import org.joml.Matrix4fStack;

public class BatchPropertyBundle extends DefaultPropertyBundle {

    private final PropertyBundle delegate;

    public BatchPropertyBundle(PropertyBundle delegate) {
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
        return this.delegate.getExportResolution(renderable);
    }

    @Override
    public void setExportResolution(Renderable<?> renderable, int exportResolution) {
        this.delegate.setExportResolution(renderable, exportResolution);
    }

    @Override
    public void buildGuiControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        final BatchRenderable<?> batchRenderable = (BatchRenderable<?>) renderable;

        this.delegate.buildGuiControls(batchRenderable.currentDelegate, screen, container);

        IsometricUI.sectionHeader(container, "batch.controls", true);
        try (IsometricUI.RowBuilder builder = IsometricUI.row(container)) {
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

        IsometricUI.dynamicLabel(container, () -> Translate.gui(
                "batch.remaining",
                Math.max(0, batchRenderable.delegates.size() - batchRenderable.currentIndex - 1),
                batchRenderable.delegates.size()
        ));
    }

    @Override
    public void applyToViewMatrix(Renderable<?> renderable, Matrix4fStack modelViewStack) {
        this.delegate.applyToViewMatrix(renderable, modelViewStack);
    }

}
