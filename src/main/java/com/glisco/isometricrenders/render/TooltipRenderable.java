package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.mixin.access.GameRendererAccessor;
import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.screen.IsometricUI;
import com.glisco.isometricrenders.screen.RenderScreen;
import com.glisco.isometricrenders.util.ExportPathSpec;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.*;
import net.minecraft.client.gui.render.pip.GuiBannerResultRenderer;
import net.minecraft.client.gui.render.pip.GuiBookModelRenderer;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.client.gui.render.pip.GuiProfilerChartRenderer;
import net.minecraft.client.gui.render.pip.GuiSignRenderer;
import net.minecraft.client.gui.render.pip.GuiSkinRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.fog.FogRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import com.mojang.math.Axis;
import org.joml.Matrix4fStack;
import org.joml.Vector2i;

import java.util.List;

public class TooltipRenderable extends DefaultRenderable<TooltipRenderable.TooltipPropertyBundle> {

    private final ItemStack stack;

    public TooltipRenderable(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public void emitVertices(PoseStack matrices, MultiBufferSource vertexConsumers, float tickDelta) {
        var client = Minecraft.getInstance();

		var imm = client.renderBuffers().bufferSource();
	    var state = new GuiRenderState();
		var atlasManager = client.getAtlasManager();
		var renderer = new GuiRenderer(state, imm,
				client.gameRenderer.getSubmitNodeStorage(),
				client.gameRenderer.getFeatureRenderDispatcher(),
				List.of(
						new GuiEntityRenderer(imm, client.getEntityRenderDispatcher()),
						new GuiSkinRenderer(imm),
						new GuiBookModelRenderer(imm),
						new GuiBannerResultRenderer(imm, atlasManager),
						new GuiSignRenderer(imm, atlasManager),
						new GuiProfilerChartRenderer(imm)
		));

	    List<ClientTooltipComponent> list = Screen.getTooltipFromItem(client, this.stack).stream().map(Component::getVisualOrderText).map(ClientTooltipComponent::create).collect(Util.toMutableList());
	    this.stack.getTooltipImage().ifPresent(datax -> list.add(list.isEmpty() ? 0 : 1, ClientTooltipComponent.create(datax)));

        MouseHandler mouse = client.mouseHandler;
        int xScale = (int) mouse.getScaledXPos(client.getWindow());
        int yScale = (int) mouse.getScaledYPos(client.getWindow());

	    new GuiGraphics(client, state, xScale, yScale)
			    .renderTooltip(client.font, list, 0, 0,
				(screenWidth, screenHeight, x, y, width, height) -> new Vector2i(DefaultTooltipPositioner.INSTANCE.positionTooltip(screenWidth, screenHeight, x, y, width, height)).add(-12 - width / 2, 12 - height / 2),
				this.stack.get(DataComponents.TOOLTIP_STYLE));

		renderer.render(((GameRendererAccessor)client.gameRenderer).isometric$getFogRenderer().getBuffer(FogRenderer.FogMode.NONE));
		renderer.close();
    }

    @Override
    public TooltipPropertyBundle properties() {
        return TooltipPropertyBundle.INSTANCE;
    }

    @Override
    public ExportPathSpec exportPath() {
        return ExportPathSpec.of("tooltip", BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
    }

    public static class TooltipPropertyBundle extends DefaultPropertyBundle {
        public static final TooltipPropertyBundle INSTANCE = new TooltipPropertyBundle();

        @Override
        public void buildGuiControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
            IsometricUI.sectionHeader(container, "transform_options", false);
            IsometricUI.intControl(container, this.scale, "scale", 10);
        }

        @Override
        public void applyToViewMatrix(Renderable<?> renderable, Matrix4fStack modelViewStack) {
            final float scale = this.scale.get() / 10000f;
            modelViewStack.scale(scale, scale, -scale);

            modelViewStack.translate(this.xOffset.get() / 260f, this.yOffset.get() / -260f, 0);
            modelViewStack.rotate(Axis.YP.rotationDegrees(180));
            modelViewStack.rotate(Axis.ZP.rotationDegrees(180));
        }
    }
}
