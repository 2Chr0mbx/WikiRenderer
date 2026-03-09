package com.pigicial.wikirenderer.render.entity.options.types;

import com.pigicial.wikirenderer.components.SearchableEntityListComponent;
import io.wispforest.owo.ui.container.CollapsibleContainer;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class EnumOverride<S extends EntityRenderState, E extends Enum<E>> extends OptionalOverride<S, E> {

    private final Class<E> enumClass;

    public EnumOverride(String key, Class<E> enumClass, Function<S, E> getter, BiConsumer<S, E> setter) {
        super(key, getter, setter, enumClass.getEnumConstants()[0]);
        this.enumClass = enumClass;
    }

    @Override
    protected void addToComponentRow(@UnknownNullability FlowLayout row) {
        CollapsibleContainer enumDropdown = UIContainers.collapsible(Sizing.content(), Sizing.content(), Component.literal("Options"), false);

        enumDropdown.child(addOption(null));
        for (E enumOption : enumClass.getEnumConstants()) {
            enumDropdown.child(addOption(enumOption));
        }
        //enumDropdown.padding(Insets.top(2));
        enumDropdown.horizontalAlignment(HorizontalAlignment.RIGHT);
        enumDropdown.titleLayout().padding(Insets.vertical(2));
        enumDropdown.titleLayout().children().get(1).margins(Insets.left(5));

        row.child(enumDropdown);
    }

    private SearchableEntityListComponent.DynamicTextButton addOption(@Nullable E enumOption) {
        return new SearchableEntityListComponent.DynamicTextButton(() -> {
            MutableComponent component = Component.literal(enumOption == null ? "None (Null)" : toDisplayName(enumOption.toString()));

            boolean isOption = getValue() == enumOption;
            ChatFormatting color = this.enabled ? (isOption ? ChatFormatting.GREEN : ChatFormatting.WHITE) : ChatFormatting.DARK_GRAY;

            return component.withStyle(color);
        }, ignored -> setValue(enumOption));
    }

    @Override
    public E getDefaultValue() {
        return enumClass.getEnumConstants()[0];
    }
}
