package com.pigicial.wikirenderer.mixin;

import com.mojang.blaze3d.opengl.GlDebug;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlDebug.class)
public class GLDebugMixin {
    @Inject(method = "printDebugLog", at = @At("HEAD"))
    private void captureGhostError(int source, int type, int id, int severity, int length, long message, long userParam, CallbackInfo ci) {
        // This is triggered INSTANTLY when the GPU complains
        String errorMsg = GLDebugMessageCallback.getMessage(length, message);
        System.err.println("!!! GL ERROR FROM BACKGROUND: " + errorMsg);

        // This will print the background ticker or thread causing the issue
        Thread.dumpStack();
    }
}