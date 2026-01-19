package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.mixin.access.ItemStackRenderStateAccessor;
import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.screen.IsometricUI;
import com.glisco.isometricrenders.screen.RenderScreen;
import com.glisco.isometricrenders.util.ExportPathSpec;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

public class ItemRenderable extends DefaultRenderable<DefaultPropertyBundle> {

    private static final ItemStackRenderState RENDER_STATE = new ItemStackRenderState();
    private static final ItemRenderablePropertyBundle PROPERTIES = new ItemRenderablePropertyBundle();

    static {
        PROPERTIES.slant.setDefaultValue(0D).setToDefault();
        PROPERTIES.rotation.setDefaultValue(0).setToDefault();
    }

    @Override
    public void setupLighting(Matrix4f modelViewMatrix) {
        if (RENDER_STATE.usesBlockLight()) {

            // pulled from Lighting's first setup for ITEMS_3D, but with the scaling value of y changed from -1.0f to 1.0f
            // ngl i have absolutely no idea why this works, but it does - it might have something to do with their atlas sheets having an inverted Y value, idk, probably does
            // (for that, see CachedOrthoProjectionMatrixBuffer and how when it's created in GuiRendered, flip y is true)
            // also i changed the numbers here to use Math.toRadians() rather than harder to process numbers
            Matrix4f matrix4f2 = new Matrix4f()
                    .scaling(1.0F, 1.0F, 1.0F) // IMPORTANT: the y is changed from -1.0 to 1.08
                    .rotateYXZ((float) Math.toRadians(62), (float) Math.toRadians(185.5), 0.0F)
                    .rotateYXZ((float) Math.toRadians(-22.5), (float) (Math.toRadians(135)), 0.0F);

            if (this.lightingBuffer == null)
                this.lightingBuffer = RenderSystem.getDevice().createBuffer(() -> "IsometricRenders DefaultRenderable Lighting UBO", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, LIGHTING_UBO_SIZE);

            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, LIGHTING_UBO_SIZE)
                        .putVec3(matrix4f2.transformDirection(new Vector3f(0.2F, 1.0F, -0.7F).normalize(), new Vector3f()))
                        .putVec3(matrix4f2.transformDirection(new Vector3f(-0.2F, 1.0F, 0.7F).normalize(), new Vector3f()))
                        .get();

                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.lightingBuffer.slice(), byteBuffer);
            }

            RenderSystem.setShaderLights(this.lightingBuffer.slice());
        } else {
            Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_FLAT);
        }
    }

    private final ItemStack stack;

    public ItemRenderable(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public void prepare() {
        Minecraft.getInstance().getItemModelResolver().appendItemLayers(
            RENDER_STATE,
            this.stack,
            ItemDisplayContext.GUI,
            Minecraft.getInstance().level,
            null,
            0
        );
    }

    @Override
    public void emitVerticesThenDraw(Matrix4fStack matrix4fStack, PoseStack matrices, MultiBufferSource vertexConsumers, float tickDelta) {
        ((ItemStackRenderStateAccessor) RENDER_STATE).isometric$setDisplayContext(ItemDisplayContext.GUI);
        RENDER_STATE.submit(matrices, Minecraft.getInstance().gameRenderer.getSubmitNodeStorage(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
    }

    @Override
    public void cleanUp() {
        RENDER_STATE.clear();
    }

    @Override
    public DefaultPropertyBundle properties() {
        return PROPERTIES;
    }

    @Override
    public ExportPathSpec exportPath() {
        return ExportPathSpec.ofIdentified(
            BuiltInRegistries.ITEM.getKey(this.stack.getItem()),
            "item"
        );
    }

    private static class ItemRenderablePropertyBundle extends DefaultPropertyBundle {
        @Override
        public void applyToViewMatrix(Renderable<?> renderable, Matrix4fStack modelViewStack) {
            float scale = (this.scale.get() / 100f) * 2f;
            modelViewStack.scale(scale, scale, scale);
        }

        @Override
        public void buildGuiControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
            IsometricUI.sectionHeader(container, "transform_options", false);
            IsometricUI.intControl(container, scale, "scale", 10);
            IsometricUI.sectionHeader(container, "item_scale_warning_1", false);
            IsometricUI.sectionHeader(container, "item_scale_warning_2", false);
        }
    }
}
