package com.pigicial.wikirenderer.render.entity.options.types;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.pigicial.wikirenderer.components.FullWidthCollapsibleContainer;
import com.pigicial.wikirenderer.components.MiniEditBoxComponent;
import com.pigicial.wikirenderer.components.SearchableEntityListComponent;
import com.pigicial.wikirenderer.textures.PlayerTextureUtils;
import com.pigicial.wikirenderer.util.Translate;
import io.wispforest.owo.ui.component.ItemComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class ItemStackOverride<S extends EntityRenderState> extends OptionalOverride<S, ItemStack> {

    private String itemName = "";
    private String playerHeadTextureID = "";

    private FullWidthCollapsibleContainer layout;
    private FlowLayout itemOptionsLayout = null;
    private ItemComponent itemIconComponent = null;

    private FlowLayout playerHeadOptionsLayout;

    public ItemStackOverride(String key, Function<S, ItemStack> getter, BiConsumer<S, ItemStack> setter) {
        super(key, getter, setter);
    }

    @Override
    public UIComponent buildComponent() {
        SearchableEntityListComponent.LeftAlignedCheckbox checkbox = new SearchableEntityListComponent.LeftAlignedCheckbox(
                Component.literal(toDisplayName(key)),
                Sizing.fill(50),
                () -> this.enabled, pressed ->
                this.enabled = pressed
        );

        this.layout = new FullWidthCollapsibleContainer(checkbox, () -> {
            ItemStack item = getValue();
            return item == null || item.isEmpty() ? Translate.gui("not_set") : item.getItemName().copy();
        }, false);
        this.layout.margins(Insets.of(0, 0, 0, 0));

        this.itemOptionsLayout = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
        this.itemOptionsLayout.horizontalAlignment(HorizontalAlignment.LEFT);
        this.itemOptionsLayout.verticalAlignment(VerticalAlignment.CENTER);

        this.itemOptionsLayout.child(UIComponents.label(Translate.gui("item")));
        this.itemOptionsLayout.child(this.buildItemNameComponent());

        ItemStack currentItem = getValue();
        if (currentItem != null && !currentItem.isEmpty()) {
            this.itemIconComponent = UIComponents.item(currentItem);
            this.itemOptionsLayout.child(this.itemIconComponent);
        }

        this.layout.child(this.itemOptionsLayout);

        this.playerHeadOptionsLayout = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
        this.playerHeadOptionsLayout.horizontalAlignment(HorizontalAlignment.LEFT);
        this.playerHeadOptionsLayout.verticalAlignment(VerticalAlignment.CENTER);
        this.playerHeadOptionsLayout.child(UIComponents.label(Translate.gui("head_texture_id")));
        this.playerHeadOptionsLayout.child(this.buildPlayerHeadTextureComponent());
        this.playerHeadOptionsLayout.id("player_head_layout");

        if (currentItem != null && currentItem.getItem() == Items.PLAYER_HEAD) {
            this.layout.child(this.playerHeadOptionsLayout);
        }
        return layout;
    }

    private MiniEditBoxComponent buildItemNameComponent() {
        MiniEditBoxComponent editBox = new MiniEditBoxComponent(Sizing.fixed(100), itemName);
        editBox.onChanged().subscribe(text -> {
            this.itemName = text;
            try {
                ItemInput result = new ItemParser(HolderLookup.Provider.create(Stream.of(BuiltInRegistries.ITEM))).parse(new StringReader(text));
                setValue(new ItemStack(result.item()));
            } catch (CommandSyntaxException e) {
                setValue(ItemStack.EMPTY);
            }
        });
        return editBox;
    }

    private MiniEditBoxComponent buildPlayerHeadTextureComponent() {
        MiniEditBoxComponent editBox = new MiniEditBoxComponent(Sizing.fixed(100), playerHeadTextureID);
        editBox.onChanged().subscribe(text -> {
            this.playerHeadTextureID = text;
            setValue(new ItemStack(Items.PLAYER_HEAD));
        });

        return editBox;
    }

    @Override
    public void setValue(@Nullable ItemStack newItem) {
        if (newItem != null && newItem.getItem() == Items.PLAYER_HEAD) {
            newItem = PlayerTextureUtils.createPlayerHead(PlayerTextureUtils.createTexturedGameProfileFromID(playerHeadTextureID));
        }

        ItemStack previousItem = getValue();
        super.setValue(newItem);
        if (!Objects.equals(newItem, previousItem)) {
            if (itemIconComponent != null) {
                this.itemOptionsLayout.removeChild(itemIconComponent);
                this.itemIconComponent = null;
            }
            if (newItem != null && !newItem.isEmpty()) {
                this.itemIconComponent = UIComponents.item(newItem);
                this.itemOptionsLayout.child(this.itemIconComponent);
            }

            if (newItem != null && newItem.getItem() == Items.PLAYER_HEAD) {
                if (layout.childById(playerHeadOptionsLayout.getClass(), "player_head_layout") == null) {
                    this.layout.child(this.playerHeadOptionsLayout);
                }
            } else {
                this.layout.removeChild(this.playerHeadOptionsLayout);
            }
        }
    }

    @Override
    protected void addToComponentRow(@UnknownNullability FlowLayout row) {

    }

    @Override
    public ItemStack getDefaultValue() {
        return ItemStack.EMPTY;
    }
}
