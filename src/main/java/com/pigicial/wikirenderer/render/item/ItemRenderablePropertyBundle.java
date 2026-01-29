package com.pigicial.wikirenderer.render.item;

import com.pigicial.wikirenderer.property.DefaultCroppablePropertyBundle;
import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.screen.WikiRendererUI;
import com.pigicial.wikirenderer.screen.RenderScreen;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.world.item.Items;
import org.joml.Matrix4fStack;

public class ItemRenderablePropertyBundle extends DefaultCroppablePropertyBundle {

    protected int playerHeadsExportResolution = 300;

    @Override
    protected int getDefaultExportResolution() {
        return 160;
    }

    @Override
    public void setExportResolution(Renderable<?> renderable, int exportResolution) {
        if (((ItemRenderable) renderable).stack.is(Items.PLAYER_HEAD)) {
            playerHeadsExportResolution = exportResolution;
        } else {
            super.setExportResolution(renderable, exportResolution);
        }
    }

    @Override
    public int getExportResolution(Renderable<?> renderable) {
        if (((ItemRenderable) renderable).stack.is(Items.PLAYER_HEAD)) {
            return playerHeadsExportResolution;
        } else {
            return super.getExportResolution(renderable);
        }
    }

    @Override
    protected boolean shouldCropByDefault() {
        return false;
    }

    @Override
    public void applyToViewMatrix(Renderable<?> renderable, Matrix4fStack modelViewStack) {
        float scale = (this.scale.get() / 100f) * 2f;
        modelViewStack.scale(scale, scale, scale);
    }

    @Override
    public void buildMainGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        WikiRendererUI.sectionHeader(container, "transform_options", false);
        WikiRendererUI.intControl(container, scale, "scale", 10);
        WikiRendererUI.sectionHeader(container, "item_scale_warning_1", true);
        WikiRendererUI.sectionHeader(container, "item_scale_warning_2", false);
    }
}
