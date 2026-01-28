package com.glisco.isometricrenders;

import com.glisco.isometricrenders.command.IsorenderCommand;
import com.glisco.isometricrenders.mixin.access.AbstractContainerScreenAccessor;
import com.glisco.isometricrenders.render.item.ItemRenderable;
import com.glisco.isometricrenders.screen.RenderScreen;
import com.glisco.isometricrenders.screen.ScreenSchedulerAndSaver;
import com.glisco.isometricrenders.screen.SelectRenderTaskScreen;
import com.glisco.isometricrenders.util.AreaSelectionHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class IsometricKeybinds {

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(IsometricRenders.MOD_ID, "keybinds"));
    public static final KeyMapping KEYBIND_SELECT = new KeyMapping("key.isometric-renders.area_select", GLFW.GLFW_KEY_C, CATEGORY);
    public static final KeyMapping KEYBIND_RENDER_HOVERED_ITEM_OR_VIEWED_ENTITY = new KeyMapping("key.isometric-renders.render_hovered_item_or_viewed_entity", GLFW.GLFW_KEY_H, CATEGORY);
    public static final KeyMapping KEYBIND_BATCH_RENDER_INVENTORY = new KeyMapping("key.isometric-renders.batch_render_inventory", GLFW.GLFW_KEY_M, CATEGORY);

    public static void registerKeyBinds() {
        KeyBindingHelper.registerKeyBinding(KEYBIND_SELECT);
        KeyBindingHelper.registerKeyBinding(KEYBIND_RENDER_HOVERED_ITEM_OR_VIEWED_ENTITY);
        KeyBindingHelper.registerKeyBinding(KEYBIND_BATCH_RENDER_INVENTORY);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (KEYBIND_SELECT.consumeClick()) {
                if (client.player.isShiftKeyDown()) {
                    AreaSelectionHelper.clear();
                } else {
                    AreaSelectionHelper.select();
                }
                return;
            }

            if (KEYBIND_RENDER_HOVERED_ITEM_OR_VIEWED_ENTITY.consumeClick()) {
                IsorenderCommand.renderTargetedEntity(null);
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            ScreenKeyboardEvents.afterKeyPress(screen).register((s, key) -> {
                if (key.key() == KeyBindingHelper.getBoundKeyOf(KEYBIND_RENDER_HOVERED_ITEM_OR_VIEWED_ENTITY).getValue()) {
                    ItemStack hoveredSlot = getHoveredSlot(client);
                    if (hoveredSlot != null) {
                        ScreenSchedulerAndSaver.openImmediately(new RenderScreen(new ItemRenderable(hoveredSlot)));
                    }
                }

                if (key.key() == KeyBindingHelper.getBoundKeyOf(KEYBIND_BATCH_RENDER_INVENTORY).getValue()) {
                    List<ItemStack> items = getItems(client);
                    if (items != null && !items.isEmpty()) {
                        Minecraft.getInstance().setScreen(new SelectRenderTaskScreen(items));
                    }
                }
            });
        });
    }

    @Nullable
    protected static ItemStack getHoveredSlot(Minecraft client) {
        Player player = client.player;
        if (player == null) {
            return null;
        }

        Screen currentScreen = client.screen;
        if (currentScreen instanceof AbstractContainerScreen<?> containerScreen) {
            Slot hoveredSlot = ((AbstractContainerScreenAccessor) containerScreen).getHoveredSlot();
            if (hoveredSlot == null) {
                return null;
            }

            ItemStack hoveredItem = hoveredSlot.getItem();
            if (!hoveredItem.isEmpty()) {
                return hoveredItem;
            }
        }

        return null;
    }

    @Nullable
    protected static List<ItemStack> getItems(Minecraft client) {
        Player player = client.player;
        if (player == null) {
            return null;
        }

        Screen currentScreen = client.screen;
        if (currentScreen instanceof AbstractContainerScreen<?> containerScreen) {
            AbstractContainerMenu menu = ((AbstractContainerScreenAccessor) containerScreen).menu();
            return menu.slots.stream().map(Slot::getItem).filter(stack -> !stack.isEmpty()).toList();
        }

        return null;
    }
}
