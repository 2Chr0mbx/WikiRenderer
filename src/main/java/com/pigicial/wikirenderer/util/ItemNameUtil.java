package com.pigicial.wikirenderer.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ItemNameUtil {

    public static String getItemDisplayName(ItemStack stack) {
        Component customName = stack.getDisplayName();
        String itemName = customName.getString();
        String sanitizedName = itemName.replaceAll("[<>:\"/\\\\|?*\\x00-\\x1F]+", "_");
        if (itemName.startsWith("[") && itemName.endsWith("]")) {
            // idk why this is how it does it
            sanitizedName = sanitizedName.substring(1, sanitizedName.length() - 1);
        }
        return sanitizedName;
    }
}
