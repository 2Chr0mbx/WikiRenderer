package com.pigicial.wikirenderer.property;

import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.screen.WikiRendererUI;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.util.ClientRenderCallback;
import com.pigicial.wikirenderer.util.Translate;
import com.mojang.math.Axis;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4fStack;

public class DefaultPropertyBundle implements PropertyBundle {

    public final IntProperty scale = IntProperty.of(100, 0, 1000);
    public final IntProperty rotation = IntProperty.of(135, 0, 360).withRollover();
    public final DoubleProperty slant = DoubleProperty.of(this.getDefaultSlant(), -90, 90);

    public final IntProperty xOffset = IntProperty.of(0, Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2);
    public final IntProperty yOffset = IntProperty.of(0, Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2);

    public final IntProperty rotationSpeed = IntProperty.of(0, 0, 720);
    public float rotationOffset = 0;
    protected boolean rotationOffsetUpdated = false;

    private int exportResolution = this.getDefaultExportResolution();

    public DefaultPropertyBundle() {
        ClientRenderCallback.EVENT.register(client -> this.rotationOffsetUpdated = false);
    }

    protected double getDefaultSlant() {
        return 30;
    }

    protected int getDefaultExportResolution() {
        return 1000;
    }

    @Override
    public int getExportResolution(Renderable<?> renderable) {
        return exportResolution;
    }

    @Override
    public void setExportResolution(Renderable<?> renderable, int exportResolution) {
        this.exportResolution = exportResolution;
    }

    public float getUsedRotation() {
        return this.rotation.get() + rotationOffset;
    }

    public double getUsedSlant() {
        return this.slant.get();
    }

    public void modifyRotation(int amount) {
        this.rotation.modify(amount);
    }

    public void modifySlant(double amount) {
        this.slant.modify(amount);
    }

    @Override
    public void buildMainGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        WikiRendererUI.sectionHeader(container, "transform_options", false);
        WikiRendererUI.intControl(container, scale, "scale", 10);
        WikiRendererUI.intControl(container, rotation, "rotation", 45);
        WikiRendererUI.doubleControl(container, slant, "slant", 30);
        WikiRendererUI.intControl(container, rotationSpeed, "rotation_speed", 5);

        WikiRendererUI.sectionHeader(container, "presets", true);
        try (WikiRendererUI.RowBuilder builder = WikiRendererUI.row(container)) {
            builder.row.child(Components.button(Translate.gui("dimetric"), (ButtonComponent button) -> {
                this.rotation.setToDefault();
                this.slant.set(30D);
            }).margins(Insets.right(5)));

            builder.row.child(Components.button(Translate.gui("isometric"), (ButtonComponent button) -> {
                this.rotation.setToDefault();
                this.slant.set(35.264);
            }));
        }
    }

    @Override
    public void applyToViewMatrix(Renderable<?> renderable, Matrix4fStack modelViewStack) {
        float scale = this.scale.get() / 100f;
        modelViewStack.scale(scale, scale, scale);

        modelViewStack.translate(this.xOffset.get() / 26000f, this.yOffset.get() / -26000f, 0);

        modelViewStack.rotate(Axis.XP.rotationDegrees(this.slant.get().floatValue()));
        modelViewStack.rotate(Axis.YP.rotationDegrees(this.rotation.get()));

        this.updateAndApplyRotationOffset(modelViewStack);
    }

    protected void updateAndApplyRotationOffset(Matrix4fStack modelViewStack) {
        if (rotationSpeed.get() != 0) {
            if (!this.rotationOffsetUpdated) {
                rotationOffset += Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaTicks() * rotationSpeed.get() * .05f;
                this.rotationOffsetUpdated = true;
            }
            modelViewStack.rotate(Axis.YP.rotationDegrees(rotationOffset));
        } else {
            rotationOffset = 0;
        }
    }
}
