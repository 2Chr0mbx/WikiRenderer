package com.pigicial.wikirenderer.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ItemBlockUtil {
    private static final ItemStackRenderState RENDER_STATE = new ItemStackRenderState();

    public static boolean doesItemUseBlockLight(ItemStack itemStack) {
        RENDER_STATE.clear();
        Minecraft.getInstance().getItemModelResolver().appendItemLayers(
                RENDER_STATE,
                itemStack,
                ItemDisplayContext.GUI,
                Minecraft.getInstance().level,
                null,
                0
        );
        return RENDER_STATE.usesBlockLight();
    }
}
