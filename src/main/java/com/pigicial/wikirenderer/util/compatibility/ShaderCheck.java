package com.pigicial.wikirenderer.util.compatibility;

import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.api.v0.IrisApi;

public class ShaderCheck {
    public static boolean isUsingShaders() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) {
            return false;
        }

        try {
            return IrisApi.getInstance().isShaderPackInUse();
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }
}
