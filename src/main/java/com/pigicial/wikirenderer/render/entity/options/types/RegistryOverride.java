package com.pigicial.wikirenderer.render.entity.options.types;

import com.pigicial.wikirenderer.components.FullWidthCollapsibleContainer;
import com.pigicial.wikirenderer.components.SearchableEntityListComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.animal.chicken.ChickenVariant;
import net.minecraft.world.entity.animal.wolf.WolfVariants;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class RegistryOverride<S extends EntityRenderState, R> extends OptionalOverride<S, R> {

    private final List<R> options = new ArrayList<>();
    private final Function<R, String> toString;
    private final boolean alwaysApply;

    public RegistryOverride(String key, ResourceKey<? extends Registry<? extends R>> registryKey, Function<S, R> getter, BiConsumer<S, R> setter, Function<R, String> toString, boolean alwaysApply) {
        super(key, getter, setter, null);
        this.alwaysApply = alwaysApply;

        HolderLookup.RegistryLookup<R> lookup = VanillaRegistries.createLookup().lookupOrThrow(registryKey);
        for (Holder.Reference<R> option : lookup.listElements().toList()) {
            if (option.isBound()) {
                this.options.add(option.value());
            }
        }

        this.toString = toString;
        this.value = getDefaultValue();
    }

    public UIComponent buildComponent() {
        SearchableEntityListComponent.LeftAlignedCheckbox checkbox = new SearchableEntityListComponent.LeftAlignedCheckbox(
                Component.literal(toDisplayName(key)),
                Sizing.fill(65),
                () -> this.enabled,
                pressed -> this.enabled = pressed
        );

        FullWidthCollapsibleContainer layout = new FullWidthCollapsibleContainer(checkbox, () -> {
            R value = getValue();
            return Component.literal(value == null ? "Not Set" : this.toString.apply(value));
        }, false);

        for (R enumOption : this.options) {
            layout.child(addOption(enumOption));
        }

        return layout;
    }

    @Override
    public void apply(S renderState) {
        if (this.enabled || this.alwaysApply) {
            this.setter.accept(renderState, value);
        }
    }

    @Override
    protected void addToComponentRow(@UnknownNullability FlowLayout row) {

    }

    private SearchableEntityListComponent.DynamicTextButton addOption(@Nullable R registryOption) {
        return new SearchableEntityListComponent.DynamicTextButton(() -> {
            MutableComponent component = Component.literal(registryOption == null ? "None (Null)" : this.toString.apply(registryOption));

            boolean isOption = getValue() == registryOption;
            ChatFormatting color = this.enabled ? (isOption ? ChatFormatting.GREEN : ChatFormatting.WHITE) : ChatFormatting.DARK_GRAY;

            return component.withStyle(color);
        }, ignored -> setValue(registryOption));
    }

    @Override
    public R getDefaultValue() {
        return options.isEmpty() ? null : options.getFirst();
    }
}
