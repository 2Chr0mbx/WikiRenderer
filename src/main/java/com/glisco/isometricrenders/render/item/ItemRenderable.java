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
import net.minecraft.network.chat.Component;
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

public class ItemRenderable extends ItemBasedRenderable<ItemRenderablePropertyBundle> implements TextureDataProvider {

    private static final ItemStackRenderState RENDER_STATE = new ItemStackRenderState();
    private static final ItemRenderablePropertyBundle PROPERTIES = new ItemRenderablePropertyBundle();

    static {
        PROPERTIES.slant.setDefaultValue(0D).setToDefault();
        PROPERTIES.rotation.setDefaultValue(0).setToDefault();
    }

    protected final ItemStack stack;

    public ItemRenderable(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public void setupLighting(Matrix4f modelViewMatrix) {
        this.setupLighting(RENDER_STATE);
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
        ExportPathSpec path = ExportPathSpec.ofIdentified(
                BuiltInRegistries.ITEM.getKey(this.stack.getItem()),
                "item"
        );

        Component customName = stack.getCustomName();
        if (customName != null) {
            path = path.differentFileName(customName.getString());
        }

        return path;

    }

    @Override
    public @NotNull Map<String, MinecraftTexturesPayload> getTextureData() {
        MinecraftTexturesPayload playerSkin = SkinGrabber.getPlayerHeadTextureData(this.stack);
        return playerSkin == null ? new HashMap<>() : Map.of("item", playerSkin);
    }
}
