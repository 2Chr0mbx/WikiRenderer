package com.pigicial.wikirenderer.mixin;

import com.pigicial.wikirenderer.screen.ScreenSchedulerAndSaver;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void openScheduled(Screen screen, CallbackInfo ci) {
        if (screen != null || !ScreenSchedulerAndSaver.hasScheduled()) return;

        ScreenSchedulerAndSaver.openScheduledScreen();
        ci.cancel();
    }
}
