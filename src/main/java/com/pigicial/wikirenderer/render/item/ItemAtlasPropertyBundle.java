package com.pigicial.wikirenderer.render.item;

import com.pigicial.wikirenderer.property.DefaultPropertyBundle;
import com.pigicial.wikirenderer.property.IntProperty;
import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.screen.WikiRendererUI;
import com.pigicial.wikirenderer.screen.RenderScreen;
import io.wispforest.owo.ui.container.FlowLayout;
import org.joml.Matrix4fStack;

public class ItemAtlasPropertyBundle extends DefaultPropertyBundle {

    public static final ItemAtlasPropertyBundle INSTANCE = new ItemAtlasPropertyBundle();

    protected final IntProperty columns = IntProperty.of(20, 1, 500);

    @Override
    public void buildMainGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        WikiRendererUI.sectionHeader(container, "transform_options", false);

        WikiRendererUI.intControl(container, this.scale, "scale", 10);
        WikiRendererUI.intControl(container, this.columns, "columns", 1);
    }

    @Override
    protected double getDefaultSlant() {
        return 0;
    }

    @Override
    protected int getDefaultRotation() {
        return 0;
    }

    @Override
    public double getUsedSlant() {
        return 0;
    }

    @Override
    public float getUsedRotation() {
        return 0;
    }

    @Override
    public boolean supportsAutomaticRotations() {
        return false;
    }

    @Override
    public void applyToViewMatrix(Renderable<?> renderable, Matrix4fStack modelViewStack) {
        float scale = this.scale.get() / 100f;
        modelViewStack.scale(scale, scale, scale);
        modelViewStack.translate(this.xOffset.get() / 26000f, this.yOffset.get() / -26000f, 0);
    }
}
