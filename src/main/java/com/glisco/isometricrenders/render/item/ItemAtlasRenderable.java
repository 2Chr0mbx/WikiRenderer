package com.glisco.isometricrenders.render.item;

import com.glisco.isometricrenders.render.DefaultRenderable;
import com.glisco.isometricrenders.util.ExportPathSpec;
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
import org.joml.Matrix4fStack;

import java.util.List;

public class ItemAtlasRenderable extends DefaultRenderable<ItemAtlasPropertyBundle> {

	private static final ItemStackRenderState RENDER_STATE = new ItemStackRenderState();
    private final Minecraft client = Minecraft.getInstance();
    private final List<ItemStack> items;
    private final String atlasSource;

    public ItemAtlasRenderable(String atlasSource, List<ItemStack> items) {
        this.atlasSource = atlasSource;
        this.items = items;
    }

    @Override
    public void emitVerticesThenDraw(Matrix4fStack matrix4fStack, PoseStack matrices, MultiBufferSource vertexConsumers, float tickDelta) {
        final int columns = this.getProperties().columns.get();
        final int rows = Mth.positiveCeilDiv(this.items.size(), columns);

        final float spacing = 1.25f;

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

	            itemModelManager.updateForTopItem(
			            RENDER_STATE,
			            this.items.get(index),
			            ItemDisplayContext.GUI,
			            this.client.level,
			            null,
			            0
	            );
	            RENDER_STATE.submit(
						matrices,
			            nodeStorage,
			            LightTexture.FULL_BRIGHT,
			            OverlayTexture.NO_OVERLAY,
			            0
	            );
            }
            matrices.popPose();
        }
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
