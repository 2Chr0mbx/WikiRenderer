package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.mixin.access.ItemStackRenderStateAccessor;
import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.util.ExportPathSpec;
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
import org.joml.Matrix4fStack;

public class ItemRenderable extends DefaultRenderable<DefaultPropertyBundle> {

    private static final ItemStackRenderState RENDER_STATE = new ItemStackRenderState();
    private static final DefaultPropertyBundle PROPERTIES = new DefaultPropertyBundle() {
        @Override
        public void applyToViewMatrix(Renderable<?> renderable, Matrix4fStack modelViewStack) {
            final float scale = (this.scale.get() / 100f) * 2f;
            modelViewStack.scale(scale, scale, scale);

            modelViewStack.translate(this.xOffset.get() / 26000f, this.yOffset.get() / -26000f, 0);

            modelViewStack.rotate(Axis.XP.rotationDegrees(this.slant.get()));
            modelViewStack.rotate(Axis.YP.rotationDegrees(this.rotation.get()));

            this.updateAndApplyRotationOffset(modelViewStack);
        }
    };

    static {
        PROPERTIES.slant.setDefaultValue(0).setToDefault();
        PROPERTIES.rotation.setDefaultValue(0).setToDefault();
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
    public void emitVertices(PoseStack matrices, MultiBufferSource vertexConsumers, float tickDelta) {
        ((ItemStackRenderStateAccessor) RENDER_STATE).isometric$setDisplayContext(ItemDisplayContext.NONE);
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

//    private static class TransformlessBakedModel extends ForwardingBakedModel {
//        public TransformlessBakedModel(BakedModel inner) {
//            this.wrapped = inner;
//        }
//
//        @Override
//        public ModelTransformation getTransformation() {
//            return ModelTransformation.NONE;
//        }
//    }
}
