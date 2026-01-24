package com.glisco.isometricrenders.render.item;

import com.glisco.isometricrenders.mixin.access.ItemStackRenderStateAccessor;
import com.glisco.isometricrenders.render.DefaultRenderable;
import com.glisco.isometricrenders.textures.SkinGrabber;
import com.glisco.isometricrenders.textures.TextureDataProvider;
import com.glisco.isometricrenders.util.ExportPathSpec;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.*;

public class ItemRenderable extends DefaultRenderable<ItemRenderablePropertyBundle> implements TextureDataProvider {

    private static final ItemStackRenderState RENDER_STATE = new ItemStackRenderState();
    private static final ItemRenderablePropertyBundle PROPERTIES = new ItemRenderablePropertyBundle();

    static {
        PROPERTIES.slant.setDefaultValue(0D).setToDefault();
        PROPERTIES.rotation.setDefaultValue(0).setToDefault();
    }

    private final ItemStack stack;

    public ItemRenderable(ItemStack stack) {
        this.stack = stack;
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
    public ItemRenderablePropertyBundle getProperties() {
        return PROPERTIES;
    }

    @Override
    public ExportPathSpec getExportPath() {
        return ExportPathSpec.ofIdentified(
            BuiltInRegistries.ITEM.getKey(this.stack.getItem()),
            "item"
        );
    }

    @Override
    public @NotNull Map<String, MinecraftTexturesPayload> getTextureData() {
        MinecraftTexturesPayload playerSkin = SkinGrabber.getPlayerHeadTextureData(this.stack);
        return playerSkin == null ? new HashMap<>() : Map.of("item", playerSkin);
    }
}
