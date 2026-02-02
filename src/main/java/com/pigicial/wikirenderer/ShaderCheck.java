package com.pigicial.wikirenderer;

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
            // Iris is not in the development environment or the game
            return false;
        }
    }
}
