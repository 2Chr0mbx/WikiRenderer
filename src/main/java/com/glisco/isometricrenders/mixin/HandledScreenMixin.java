package com.glisco.isometricrenders.mixin;

import com.glisco.isometricrenders.screen.SelectRenderTaskScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen {

    @Shadow
    @Final
    protected T handler;

    @Shadow
    public abstract void close();

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void renderInventory(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (input.key() != GLFW.GLFW_KEY_F12 || !input.hasCtrl()) return;

        this.close();
        client.setScreen(new SelectRenderTaskScreen(handler.slots.stream().map(Slot::getStack).filter(stack -> !stack.isEmpty()).toList()));

        cir.setReturnValue(false);
    }

}
