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
        if (this.delegate instanceof DefaultPropertyBundle defaultPropertyBundle) {
            this.scale.copyFrom(defaultPropertyBundle.scale);
            this.rotation.copyFrom(defaultPropertyBundle.rotation);
            this.slant.copyFrom(defaultPropertyBundle.slant);
            this.xOffset.copyFrom(defaultPropertyBundle.xOffset);
            this.yOffset.copyFrom(defaultPropertyBundle.yOffset);

            this.scale.listen(defaultPropertyBundle.scale);
            this.rotation.listen(defaultPropertyBundle.rotation);
            this.slant.listen(defaultPropertyBundle.slant);
            this.xOffset.listen(defaultPropertyBundle.xOffset);
            this.yOffset.listen(defaultPropertyBundle.yOffset);
        }
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
