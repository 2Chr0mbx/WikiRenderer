package com.glisco.isometricrenders.render.item;

import com.glisco.isometricrenders.mixin.access.GameRendererAccessor;
import com.glisco.isometricrenders.render.DefaultRenderable;
import com.glisco.isometricrenders.util.ExportPathSpec;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.*;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fStack;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.List;

public class TooltipRenderable extends DefaultRenderable<TooltipPropertyBundle> {

    private final ItemStack stack;

    public TooltipRenderable(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public void emitVerticesThenDraw(Matrix4fStack matrix4fStack, PoseStack matrices, MultiBufferSource vertexConsumers, float tickDelta) {
        Minecraft client = Minecraft.getInstance();

		MultiBufferSource.BufferSource bufferSource = client.renderBuffers().bufferSource();
	    GuiRenderState state = new GuiRenderState();
		AtlasManager atlasManager = client.getAtlasManager();

        List<PictureInPictureRenderer<?>> renderers = List.of(
                new GuiEntityRenderer(bufferSource, client.getEntityRenderDispatcher()),
                new GuiSkinRenderer(bufferSource),
                new GuiBookModelRenderer(bufferSource),
                new GuiBannerResultRenderer(bufferSource, atlasManager),
                new GuiSignRenderer(bufferSource, atlasManager),
                new GuiProfilerChartRenderer(bufferSource)
        );

        GuiRenderer renderer = new GuiRenderer(state, bufferSource, client.gameRenderer.getSubmitNodeStorage(), client.gameRenderer.getFeatureRenderDispatcher(), renderers);

	    List<ClientTooltipComponent> list = Screen.getTooltipFromItem(client, this.stack).stream().map(Component::getVisualOrderText).map(ClientTooltipComponent::create).collect(Util.toMutableList());
	    this.stack.getTooltipImage().ifPresent(datax -> list.add(list.isEmpty() ? 0 : 1, ClientTooltipComponent.create(datax)));

        MouseHandler mouse = client.mouseHandler;
        int xScale = (int) mouse.getScaledXPos(client.getWindow());
        int yScale = (int) mouse.getScaledYPos(client.getWindow());

        GuiGraphics guiGraphics = new GuiGraphics(client, state, xScale, yScale);
        guiGraphics.renderTooltip(client.font, list, 0, 0, TooltipRenderable::positionTooltip, this.stack.get(DataComponents.TOOLTIP_STYLE));

		renderer.render(((GameRendererAccessor) client.gameRenderer).isometric$getFogRenderer().getBuffer(FogRenderer.FogMode.NONE));
		renderer.close();
    }

    private static Vector2ic positionTooltip(int screenWidth, int screenHeight, int x, int y, int width, int height) {
        return new Vector2i(DefaultTooltipPositioner.INSTANCE.positionTooltip(screenWidth, screenHeight, x, y, width, height)).add(-12 - width / 2, 12 - height / 2);
    }

    public int getTooltipSize() {
        Minecraft minecraft = Minecraft.getInstance();
        List<ClientTooltipComponent> list = Screen.getTooltipFromItem(minecraft, this.stack)
                .stream()
                .map(Component::getVisualOrderText)
                .map(ClientTooltipComponent::create)
                .collect(Util.toMutableList());

        this.stack.getTooltipImage().ifPresent(data -> list.add(list.isEmpty() ? 0 : 1, ClientTooltipComponent.create(data)));

        int width = 0;
        int height = list.size() == 1 ? -2 : 0;

        for (ClientTooltipComponent component : list) {
            int componentWidth = component.getWidth(minecraft.font);
            if (componentWidth > width) width = componentWidth;
            height += component.getHeight(minecraft.font);
        }

        return Math.max(width + 8, height + 8);
    }

    @Override
    public TooltipPropertyBundle getProperties() {
        return TooltipPropertyBundle.INSTANCE;
    }

    @Override
    public ExportPathSpec getExportPath() {
        return ExportPathSpec.of("tooltip", BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
    }

}
