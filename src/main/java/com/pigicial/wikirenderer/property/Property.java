package com.pigicial.wikirenderer.property;

import com.pigicial.wikirenderer.screen.RenderScreen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class Property<T> implements BiConsumer<Property<T>, T> {

    protected T defaultValue;
    protected T value;
    protected final Map<RenderScreen, List<BiConsumer<Property<T>, T>>> changeListeners;

    public Property(T defaultValue) {
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.changeListeners = new HashMap<>();
    }

    public static <T> Property<T> of(T defaultValue) {
        return new Property<>(defaultValue);
    }

    public void set(T value) {
        this.value = value;
        this.invokeListeners();
    }

    public Property<T> setDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public void setToDefault() {
        this.value = this.defaultValue;
        this.invokeListeners();
    }

    public boolean isDefault() {
        return this.value == this.defaultValue;
    }

    public void instantListen(RenderScreen screen, BiConsumer<Property<T>, T> listener) {
        this.changeListeners.computeIfAbsent(screen, o -> new ArrayList<>()).add(listener);
        screen.registerPropertyListener(this);
        listener.accept(this, this.value);
    }

    public void futureListen(RenderScreen screen, BiConsumer<Property<T>, T> listener) {
        this.changeListeners.computeIfAbsent(screen, o -> new ArrayList<>()).add(listener);
        screen.registerPropertyListener(this);
    }

    public void addRebuildListener(RenderScreen screen) {
        this.futureListen(screen, (p, t) -> screen.guiRebuildScheduled = true);
    }

    public void removeListeners(RenderScreen renderScreen) {
        List<BiConsumer<Property<T>, T>> list = changeListeners.remove(renderScreen);
        if (list != null) {
            list.clear();
        }
    }

    public T get() {
        return value;
    }

    public void copyFrom(Property<T> source) {
        this.defaultValue = source.defaultValue;
        this.value = source.value;
        this.invokeListeners();
    }

    protected void invokeListeners() {
        this.changeListeners.values().forEach(list -> list.forEach(tConsumer -> tConsumer.accept(this, this.value)));
    }

    @Override
    public void accept(Property<T> tProperty, T t) {
        this.set(t);
    }
}
