package com.pigicial.wikirenderer.render.item;

import com.pigicial.wikirenderer.render.export.ExportPathSpec;
import com.pigicial.wikirenderer.screen.RenderScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.ArrayList;
import java.util.List;

public class ItemAtlasRenderable extends ItemBasedRenderable<ItemAtlasPropertyBundle> {

    private final Minecraft client = Minecraft.getInstance();
    private final List<ItemStack> items;
    private final List<ItemStackRenderState> renderStates; // there's probably a better fix for this than using multiple render states, but it works for now
    private final String atlasSource;

    public ItemAtlasRenderable(String atlasSource, List<ItemStack> items) {
        this.atlasSource = atlasSource;
        this.items = items;
        this.renderStates = new ArrayList<>();
        items.stream().map(item -> new ItemStackRenderState()).forEach(this.renderStates::add);
    }

    @Override
    public void emitVerticesThenDraw(RenderScreen renderScreen, Matrix4fStack matrix4fStack, PoseStack matrices, float tickDelta) {
        int columns = this.getProperties().columns.get();
        int rows = Mth.positiveCeilDiv(this.items.size(), columns);

        float spacing = 1.25f;

        matrices.scale(.1f, .1f, .1f);
        matrices.translate((-columns / 2f) * spacing - spacing / 2, (rows / 2f) * spacing + spacing / 2, 0);

		SubmitNodeStorage nodeStorage = this.client.gameRenderer.getSubmitNodeStorage();
	    ItemModelResolver itemModelManager = this.client.getItemModelResolver();
	    for (int row = 0; row < rows; row++) {
            matrices.translate(0, -spacing, 0);
            matrices.pushPose();
            for (int column = 0; column < columns; column++) {
                matrices.translate(spacing, 0, 0);
                int index = row * columns + column;
                if (index >= this.items.size()) continue;

                ItemStack itemStack = this.items.get(index);
                ItemStackRenderState renderState = this.renderStates.get(index);

                this.setupLighting(renderState);

                itemModelManager.updateForTopItem(
                        renderState,
                        itemStack,
			            ItemDisplayContext.GUI,
			            this.client.level,
			            null,
			            0
	            );
                renderState.submit(
						matrices,
			            nodeStorage,
			            LightTexture.FULL_BRIGHT,
			            OverlayTexture.NO_OVERLAY,
			            0
	            );
                // draw each loop so lighting works (maybe there's a better solution to this, can't be asked to look right now)
                this.drawSubmittedRenderFeatures();
            }
            matrices.popPose();
        }
    }

    @Override
    public void setupLighting() {

    }

    @Override
    public ItemAtlasPropertyBundle getProperties() {
        return ItemAtlasPropertyBundle.INSTANCE;
    }

    @Override
    public ExportPathSpec getExportPath() {
        return ExportPathSpec.of("atlases", this.atlasSource);
    }

}
