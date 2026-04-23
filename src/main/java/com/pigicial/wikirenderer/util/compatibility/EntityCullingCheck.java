package com.pigicial.wikirenderer.util.compatibility;

import dev.tr7zw.entityculling.EntityCullingMod;
import net.fabricmc.loader.api.FabricLoader;

public class EntityCullingCheck {
    public static boolean isUsingEntityCulling() {
        if (!FabricLoader.getInstance().isModLoaded("entityculling")) {
            return false;
        }

        try {
            return EntityCullingMod.enabled;
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }
}
