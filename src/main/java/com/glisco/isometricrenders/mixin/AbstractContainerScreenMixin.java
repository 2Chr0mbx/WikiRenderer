package com.glisco.isometricrenders.mixin;

import com.glisco.isometricrenders.screen.SelectRenderTaskScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> extends Screen {

    @Shadow
    @Final
    protected T menu;

    @Shadow
    public abstract void onClose();

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void renderInventory(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        System.out.println("key pressed");
        if (input.key() != GLFW.GLFW_KEY_F12 || !input.hasControlDown()) return;

        this.onClose();
        minecraft.setScreen(new SelectRenderTaskScreen(menu.slots.stream().map(Slot::getItem).filter(stack -> !stack.isEmpty()).toList()));

        cir.setReturnValue(false);
    }

}
