package com.glisco.isometricrenders.mixin;

import com.glisco.isometricrenders.screen.ScreenSchedulerAndSaver;
import com.glisco.isometricrenders.util.ClientRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void openScheduled(Screen screen, CallbackInfo ci) {
        if (screen != null || !ScreenSchedulerAndSaver.hasScheduled()) return;

        ScreenSchedulerAndSaver.openScheduledScreen();
        ci.cancel();
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/DeltaTracker$Timer;advanceTime(JZ)I"))
    private void onRenderStart(boolean tick, CallbackInfo ci) {
        ClientRenderCallback.EVENT.invoker().onRenderStart((Minecraft) (Object) this);
    }

}
