package com.pigicial.wikirenderer.render.entity.options.types;

import com.pigicial.wikirenderer.components.SearchableEntityListComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class OptionalOverride<S extends EntityRenderState, T> {
    protected final String key;
    private final Function<S, T> getter;
    private final BiConsumer<S, T> setter;

    private T value;
    protected boolean enabled = false;

    public OptionalOverride(String key, Function<S, T> getter, BiConsumer<S, T> setter) {
        this.key = key;
        this.getter = getter;
        this.setter = setter;
        this.value = getDefaultValue();
    }

    public OptionalOverride(String key, Function<S, T> getter, BiConsumer<S, T> setter, T defaultValue) {
        this.key = key;
        this.getter = getter;
        this.setter = setter;
        this.value = defaultValue;
    }

    public abstract T getDefaultValue();

    public void setValue(T value) {
        this.enabled = true;
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void apply(S renderState) {
        if (enabled) {
            this.setter.accept(renderState, value);
        }
    }

    public void copyFromRenderState(S renderState) {
        this.value = this.getter.apply(renderState);
    }

    public UIComponent buildComponent() {
        GridLayout layout = UIContainers.grid(Sizing.expand(100), Sizing.content(), 1, 2);
        layout.margins(Insets.of(0, 0, 0, 0));
        layout.horizontalAlignment(HorizontalAlignment.LEFT);
        layout.verticalAlignment(VerticalAlignment.CENTER);

        layout.child(new SearchableEntityListComponent.LeftAlignedCheckbox(Component.literal(toDisplayName(key)), () -> this.enabled, pressed -> this.enabled = pressed), 0, 0);

        FlowLayout controlLayout = UIContainers.horizontalFlow(Sizing.expand(50), Sizing.content());
        controlLayout.horizontalAlignment(HorizontalAlignment.RIGHT);

        this.addToComponentRow(controlLayout);
        layout.child(controlLayout, 0, 1);
        return layout;
    }

    public static String toDisplayName(String input) {
        return Arrays.stream(input
                        .replaceAll("([a-z])([A-Z])", "$1_$2")       // camelCase -> snake
                        .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2") // CLAYFish -> CLAY_Fish
                        .toLowerCase()
                        .split("_")
                )
                .filter(w -> !w.isEmpty())
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    protected abstract void addToComponentRow(FlowLayout row);
}
