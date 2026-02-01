package com.pigicial.wikirenderer.render.item;

import com.pigicial.wikirenderer.mixin.access.ItemStackRenderStateAccessor;
import com.pigicial.wikirenderer.textures.SkinGrabber;
import com.pigicial.wikirenderer.textures.TextureDataProvider;
import com.pigicial.wikirenderer.render.export.ExportPathSpec;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.HashMap;
import java.util.Map;

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
        ((ItemStackRenderStateAccessor) RENDER_STATE).wikirenderer$setDisplayContext(ItemDisplayContext.GUI);
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
    public @Nullable String getDefaultCustomFileName() {
        Component customName = stack.getDisplayName();
        String itemName = customName.getString();
        String sanitizedName = itemName.replaceAll("[<>:\"/\\\\|?*\\x00-\\x1F]+", "_");
        if (itemName.startsWith("[") && itemName.endsWith("]")) {
            // idk why this is how it does it
            sanitizedName = sanitizedName.substring(1, sanitizedName.length() - 1);
        }
        return sanitizedName;
    }

    @Override
    public @NotNull Map<String, MinecraftTexturesPayload> getTextureData() {
        MinecraftTexturesPayload playerSkin = SkinGrabber.getPlayerHeadTextureData(this.stack);
        return playerSkin == null ? new HashMap<>() : Map.of("item", playerSkin);
    }
}
