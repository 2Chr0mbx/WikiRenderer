package com.pigicial.wikirenderer.compat;

import me.shedaniel.rei.api.client.REIRuntime;
import me.shedaniel.rei.api.client.gui.widgets.TextField;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

public class REISearchFocus {

    private static Method cachedIsFocused = null;
    private static boolean initialized = false;

    // isVisible and hasBorder are stable unobfuscated names on TextFieldWidget.
    // The only remaining boolean no-arg method is isFocused(), obfuscated by REI
    // (see shedaniel/RoughlyEnoughItems#1818).
    private static final Set<String> EXCLUDED = Set.of("isVisible", "hasBorder");

    public static boolean isSearchFieldFocused() {
        try {
            REIRuntime runtime = REIRuntime.getInstance();
            if (runtime == null) return false;

            TextField searchField = runtime.getSearchTextField();
            if (searchField == null) return false;

            if (!initialized) {
                initialized = true;
                cachedIsFocused = resolveIsFocused(searchField.getClass());
            }

            return cachedIsFocused != null && (boolean) cachedIsFocused.invoke(searchField);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Walks the class hierarchy to find TextFieldWidget, then returns the single
     * boolean no-arg method that is neither isVisible nor hasBorder — that is
     * isFocused() regardless of its obfuscated runtime name.
     */
    private static Method resolveIsFocused(Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            if (current.getSimpleName().contains("TextFieldWidget")) {
                return Arrays.stream(current.getDeclaredMethods())
                    .filter(m -> m.getParameterCount() == 0)
                    .filter(m -> m.getReturnType() == boolean.class)
                    .filter(m -> !EXCLUDED.contains(m.getName()))
                    .findFirst()
                    .map(m -> { m.setAccessible(true); return m; })
                    .orElse(null);
            }
            current = current.getSuperclass();
        }
        return null;
    }
}