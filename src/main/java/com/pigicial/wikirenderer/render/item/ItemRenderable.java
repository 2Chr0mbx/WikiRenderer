package com.pigicial.wikirenderer.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.mixin.access.ItemStackRenderStateAccessor;
import com.pigicial.wikirenderer.render.batch.DynamicBatchLabelProvider;
import com.pigicial.wikirenderer.render.export.ExportPathSpec;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.textures.PlayerTextureUtils;
import com.pigicial.wikirenderer.textures.TextureData;
import com.pigicial.wikirenderer.textures.TextureDataProvider;
import com.pigicial.wikirenderer.util.ItemNameUtil;
import com.pigicial.wikirenderer.util.AnimationTimingUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4fStack;

import java.util.*;

public class ItemRenderable extends ItemBasedRenderable<ItemRenderablePropertyBundle> implements TextureDataProvider, DynamicBatchLabelProvider, AnimationTimingsProvider {

    public static final ItemRenderablePropertyBundle PROPERTIES = new ItemRenderablePropertyBundle();
    private static final ItemStackRenderState RENDER_STATE = new ItemStackRenderState();

    static {
        PROPERTIES.slant.setDefaultValue(0D).setToDefault();
        PROPERTIES.rotation.setDefaultValue(0).setToDefault();
    }

    public final ItemStack stack;
    private Map<String, TextureData> textureData = null;

    public ItemRenderable(ItemStack stack) {
        this.stack = stack;
        this.customFileName = ItemNameUtil.getItemDisplayName(this.stack);
    }

    @Override
    public String buildFileName(String preset) {
        String id = BuiltInRegistries.ITEM.getKey(this.stack.getItem()).getPath();
        String name = ItemNameUtil.getItemDisplayName(this.stack);
        return preset.replace("%id%", id).replace("%name%", name);
    }

    @Override
    public Collection<String> buildPresetExamples() {
        return List.of("label_example.item_id", "label_example.item_name");
    }

    @Override
    public ExportPathSpec getExportPath() {
        return ExportPathSpec.ofIdentified(
                BuiltInRegistries.ITEM.getKey(this.stack.getItem()),
                "item"
        );
    }

    @Override
    public void setupLighting() {
        this.setupLighting(RENDER_STATE);
    }

    @Override
    public void prepare() {
        if (getProperties().forceEnchantmentGlints.get()) {
            WikiRenderer.overrideGlint = true;
        }
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
    public void emitVerticesThenDraw(RenderScreen renderScreen, Matrix4fStack matrix4fStack, PoseStack matrices, float tickDelta) {

        ((ItemStackRenderStateAccessor) RENDER_STATE).wikirenderer$setDisplayContext(ItemDisplayContext.GUI);
        RENDER_STATE.submit(matrices, Minecraft.getInstance().gameRenderer.getSubmitNodeStorage(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
    }

    @Override
    public void cleanUp() {
        RENDER_STATE.clear();
        WikiRenderer.overrideGlint = false;
    }

    @Override
    public ItemRenderablePropertyBundle getProperties() {
        return PROPERTIES;
    }

    @Override
    public void cacheTextureData(Runnable rebuildCallback) {
        TextureData playerSkin = PlayerTextureUtils.getTextureDataFromPlayerHead(this.stack);
        this.textureData = playerSkin == null ? new HashMap<>() : Map.of("item", playerSkin);
    }

    public @NotNull Map<String, TextureData> getTextureData(Runnable rebuildCallback) {
        if (this.textureData == null) {
            this.cacheTextureData(rebuildCallback);
        }

        TextureData playerSkin = PlayerTextureUtils.getTextureDataFromPlayerHead(this.stack);
        return playerSkin == null ? new HashMap<>() : Map.of("item", playerSkin);
    }

    @Override
    public List<Integer> getTicksToFullyAnimate() {
        List<Integer> animationTimings = new LinkedList<>();
        AnimationTimingUtil.scanTicksToFullyAnimateItem(stack, animationTimings);
        return animationTimings;
    }
}
