package com.pigicial.wikirenderer;

import com.pigicial.wikirenderer.command.subcommands.RenderEntitySubCommand;
import com.pigicial.wikirenderer.mixin.access.AbstractContainerScreenAccessor;
import com.pigicial.wikirenderer.mixin.access.CreativeModeInventoryScreenAccessor;
import com.pigicial.wikirenderer.render.item.ItemRenderable;
import com.pigicial.wikirenderer.render.item.TooltipRenderable;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.screen.ScreenSchedulerAndSaver;
import com.pigicial.wikirenderer.screen.SelectRenderTaskScreen;
import com.pigicial.wikirenderer.util.AreaSelectionHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class WikiRendererKeybinds {

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(WikiRenderer.MOD_ID, "keybinds"));
    public static final KeyMapping KEYBIND_SELECT_AREA = new KeyMapping("key.wikirenderer.area_select", GLFW.GLFW_KEY_C, CATEGORY);
    public static final KeyMapping KEYBIND_SELECT_AREA_EXPAND = new KeyMapping("key.wikirenderer.area_select_expand", GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping KEYBIND_RENDER_HOVERED_ITEM_OR_VIEWED_ENTITY = new KeyMapping("key.wikirenderer.render_hovered_item_or_viewed_entity", GLFW.GLFW_KEY_H, CATEGORY);
    public static final KeyMapping KEYBIND_RENDER_HOVERED_ITEM_TOOLTIP = new KeyMapping("key.wikirenderer.render_hovered_item_tooltip", GLFW.GLFW_KEY_J, CATEGORY);
    public static final KeyMapping KEYBIND_BATCH_RENDER_INVENTORY = new KeyMapping("key.wikirenderer.batch_render_inventory", GLFW.GLFW_KEY_K, CATEGORY);

    public static void registerKeyBinds() {
        KeyBindingHelper.registerKeyBinding(KEYBIND_SELECT_AREA);
        KeyBindingHelper.registerKeyBinding(KEYBIND_SELECT_AREA_EXPAND);
        KeyBindingHelper.registerKeyBinding(KEYBIND_RENDER_HOVERED_ITEM_OR_VIEWED_ENTITY);
        KeyBindingHelper.registerKeyBinding(KEYBIND_BATCH_RENDER_INVENTORY);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (KEYBIND_SELECT_AREA.consumeClick()) {
                if (client.player.isShiftKeyDown()) {
                    AreaSelectionHelper.clear();
                } else {
                    AreaSelectionHelper.select();
                }
                return;
            }
            if (KEYBIND_SELECT_AREA_EXPAND.consumeClick()) {
                if (client.player.isShiftKeyDown()) {
                    AreaSelectionHelper.clear();
                } else {
                    AreaSelectionHelper.expand();
                }
                return;
            }

            if (KEYBIND_RENDER_HOVERED_ITEM_OR_VIEWED_ENTITY.consumeClick()) {
                RenderEntitySubCommand.renderTargetedEntity(null);
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> ScreenKeyboardEvents.afterKeyPress(screen).register((s, key) -> {
            if (key.key() == KeyBindingHelper.getBoundKeyOf(KEYBIND_RENDER_HOVERED_ITEM_OR_VIEWED_ENTITY).getValue()) {
                ItemStack hoveredSlot = getHoveredSlot(client);
                if (hoveredSlot != null) {
                    ScreenSchedulerAndSaver.openImmediately(new RenderScreen(new ItemRenderable(hoveredSlot)));
                }
            }

            if (key.key() == KeyBindingHelper.getBoundKeyOf(KEYBIND_RENDER_HOVERED_ITEM_TOOLTIP).getValue()) {
                ItemStack hoveredSlot = getHoveredSlot(client);
                if (hoveredSlot != null) {
                    ScreenSchedulerAndSaver.openImmediately(new RenderScreen(new TooltipRenderable(hoveredSlot)));
                }
            }

            if (key.key() == KeyBindingHelper.getBoundKeyOf(KEYBIND_BATCH_RENDER_INVENTORY).getValue()) {
                List<ItemStack> items = getItems(client);
                if (items != null && !items.isEmpty()) {
                    Minecraft.getInstance().setScreen(new SelectRenderTaskScreen(items));
                }
            }
        }));
    }

    @Nullable
    protected static ItemStack getHoveredSlot(Minecraft client) {
        Player player = client.player;
        if (player == null) {
            return null;
        }

        Screen currentScreen = client.screen;
        if (currentScreen instanceof AbstractContainerScreen<?> containerScreen) {
            if (currentScreen.getFocused() instanceof EditBox) return null;
            if (currentScreen instanceof CreativeModeInventoryScreen
                && CreativeModeInventoryScreenAccessor.getSelectedTab() != null
                && CreativeModeInventoryScreenAccessor.getSelectedTab().getType() == CreativeModeTab.Type.SEARCH) {
                return null;
            }

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
            if (currentScreen.getFocused() instanceof EditBox) return null;
            if (currentScreen instanceof CreativeModeInventoryScreen
                && CreativeModeInventoryScreenAccessor.getSelectedTab() != null
                && CreativeModeInventoryScreenAccessor.getSelectedTab().getType() == CreativeModeTab.Type.SEARCH) {
                return null;
            }

            AbstractContainerMenu menu = ((AbstractContainerScreenAccessor) containerScreen).menu();
            return menu.slots.stream().map(Slot::getItem).filter(stack -> !stack.isEmpty()).toList();
        }

        return null;
    }
}
