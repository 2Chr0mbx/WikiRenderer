package com.pigicial.wikirenderer.render.batch;

import com.pigicial.wikirenderer.property.DefaultPropertyBundle;
import com.pigicial.wikirenderer.property.IntProperty;
import com.pigicial.wikirenderer.property.Property;
import com.pigicial.wikirenderer.property.PropertyBundle;
import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.render.item.ItemRenderable;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.screen.WikiRendererUI;
import com.pigicial.wikirenderer.util.Translate;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import org.joml.Matrix4fStack;

public class BatchPropertyBundle extends DefaultPropertyBundle {

    private static final IntProperty ITEM_RESOLUTION_PROPERTY = IntProperty.of(160, 1, Short.MAX_VALUE / 2);
    private static final IntProperty PLAYER_HEAD_RESOLUTION_PROPERTY = IntProperty.of(300, 1, Short.MAX_VALUE / 2);
    public static final Property<Boolean> EXPORT_AS_ANIMATIONS = Property.of(false);
    public static String fileNameFormatter = "%name%";

    private final BatchRenderable<?> batchRenderable;
    private final PropertyBundle actualProperties;

    public BatchPropertyBundle(BatchRenderable<?> batchRenderable, PropertyBundle actualProperties) {
        this.batchRenderable = batchRenderable;
        this.actualProperties = actualProperties;

        // A bit ugly, but we copy all property values from the delegate and hook
        // the delegate onto our properties - this makes sure we don't always reset
        // the properties and that the mouse and keyboard controls actually affect the delegate
        if (this.actualProperties instanceof DefaultPropertyBundle clonedFrom) {
            this.scale.copyFrom(clonedFrom.scale);
            this.rotation.copyFrom(clonedFrom.rotation);
            this.rotationSpeed.copyFrom(clonedFrom.rotationSpeed);
            this.slant.copyFrom(clonedFrom.slant);
            this.xOffset.copyFrom(clonedFrom.xOffset);
            this.yOffset.copyFrom(clonedFrom.yOffset);

            this.scale.instantListen(clonedFrom.scale);
            this.rotation.instantListen(clonedFrom.rotation);
            this.rotationSpeed.instantListen(clonedFrom.rotationSpeed);
            this.slant.instantListen(clonedFrom.slant);
            this.xOffset.instantListen(clonedFrom.xOffset);
            this.yOffset.instantListen(clonedFrom.yOffset);
        }
    }

    @Override
    public void onRenderStart() {
        this.actualProperties.onRenderStart();
    }

    @Override
    public void applyToViewMatrix(Renderable<?> ignored, Matrix4fStack modelViewStack) {
        this.actualProperties.applyToViewMatrix(this.batchRenderable.currentDelegate, modelViewStack);
    }

    @Override
    public float updateAndGetSpinningRotationOffset() {
        if (this.batchRenderable.currentDelegate instanceof DefaultPropertyBundle delegateProperties) {
            this.rotationOffset = delegateProperties.updateAndGetSpinningRotationOffset();
            return this.rotationOffset;
        } else {
            return 0;
        }
    }

    @Override
    public void buildMainGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        BatchRenderable<?> batchRenderable = (BatchRenderable<?>) renderable;

        this.actualProperties.buildMainGUIControls(batchRenderable.currentDelegate, screen, container);

        WikiRendererUI.sectionHeader(container, "batch.controls", true);
        WikiRendererUI.booleanControl(container, EXPORT_AS_ANIMATIONS, "batch.export_as_animations");

        try (WikiRendererUI.RowBuilder builder = WikiRendererUI.autoNewLineRow(container)) {
            ButtonComponent startButton = UIComponents.button(Translate.gui("batch.start"), button -> {
                batchRenderable.start();
                button.active = false;
            });
            builder.row.child(startButton.horizontalSizing(Sizing.content(20)));
            builder.row.child(UIComponents.button(Translate.gui("batch.reset"), button -> {
                batchRenderable.reset(screen);
                startButton.active = true;
            }));
            builder.row.child(UIComponents.button(Translate.gui("batch.previous"), button -> batchRenderable.decreaseIndex()));
            builder.row.child(UIComponents.button(Translate.gui("batch.next"), button -> batchRenderable.increaseIndex()));
        }

        WikiRendererUI.dynamicLabel(container, () -> Translate.gui(
                "batch.remaining",
                Math.max(0, batchRenderable.delegates.size() - batchRenderable.currentIndex - 1),
                batchRenderable.delegates.size()
        ));
    }

    @Override
    public void buildRenderOptionGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        this.actualProperties.buildRenderOptionGUIControls(renderable, screen, container);
    }

    @Override
    public int getExportResolution(Renderable<?> ignored) {
        if (this.batchRenderable.currentDelegate instanceof ItemRenderable itemRenderable) {
            if (itemRenderable.stack.is(Items.PLAYER_HEAD)) {
                return PLAYER_HEAD_RESOLUTION_PROPERTY.get();
            } else {
                return ITEM_RESOLUTION_PROPERTY.get();
            }
        }
        return super.getExportResolution(this.batchRenderable); // this.batchRenderable is not needed here really
    }

    @Override
    public void buildExportResolutionGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        if (this.batchRenderable.currentDelegate instanceof ItemRenderable) {
            WikiRendererUI.labelledTextField(container, ITEM_RESOLUTION_PROPERTY, "item_resolution", Sizing.fixed(50));
            WikiRendererUI.labelledTextField(container, PLAYER_HEAD_RESOLUTION_PROPERTY, "player_head_resolution", Sizing.fixed(50));
        } else {
            super.buildExportResolutionGUIControls(this.batchRenderable.currentDelegate, screen, container);
        }
    }

    @Override
    public void buildFileNameGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        screen.fileNameField = WikiRendererUI.labelledTextField(container, fileNameFormatter, "batch.file_name_preset", Sizing.fixed(180));
        screen.fileNameField.setFilter(s -> s.matches("^[^<>:\"/\\\\|?*\\x00-\\x1F]*$")); // file name regex
        screen.fileNameField.setResponder(renderable::setCustomFileName);

        BatchRenderable<?> batchRenderable = (BatchRenderable<?>) renderable;
        if (!batchRenderable.delegates.isEmpty() && this.batchRenderable.currentDelegate instanceof DynamicBatchLabelProvider labelProvider) {
            WikiRendererUI.sectionHeader(container, "batch.label_presets", 10);
            for (String exampleKey : labelProvider.buildPresetExamples()) {
                WikiRendererUI.sectionHeader(container, exampleKey, false);
            }

            WikiRendererUI.sectionHeader(container, "batch.name_previews", 10);
            int delegatesAmount = batchRenderable.delegates.size();
            for (int i = 0; i < Math.min(delegatesAmount, 3); i++) {
                int index = i;
                WikiRendererUI.dynamicLabel(container, () -> {
                    int newIndex = (index + Math.max(batchRenderable.currentIndex, 0)) % delegatesAmount;
                    return Component.literal("- " + ((DynamicBatchLabelProvider) batchRenderable.delegates.get(newIndex)).buildFileName(fileNameFormatter));
                });
            }
        }
    }
}
