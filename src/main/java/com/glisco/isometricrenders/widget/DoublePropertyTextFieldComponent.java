package com.glisco.isometricrenders.widget;

import com.glisco.isometricrenders.property.DoubleProperty;
import com.glisco.isometricrenders.property.IntProperty;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.text.NumberFormat;
import java.util.Objects;
import java.util.function.Predicate;

public class DoublePropertyTextFieldComponent extends EditBox {

    private final DoubleProperty setting;
    private String content = "";

    public DoublePropertyTextFieldComponent(Sizing horizontalSizing, DoubleProperty setting) {
        super(Minecraft.getInstance().font, 0, 0, 70, 20, Component.empty());
        this.setting = setting;

        this.horizontalSizing(horizontalSizing);

        this.setValue(String.valueOf(setting.get()));
        this.setFilter(makeMatcher());

        this.setting.listen((doubleSetting, value) -> {
            this.setValue(String.valueOf(value));
        });

        this.setResponder(s -> {
            if (Objects.equals(s, content) || s.isEmpty() || s.equals("-")) {
                return;
            }

            this.content = s;
            if (s.endsWith(".")) {
                s = s.substring(0, s.length() - 1);
            }
            this.setting.set(Double.parseDouble(s));
        });
    }

    private Predicate<String> makeMatcher() {
        StringBuilder builder = new StringBuilder();
        if (this.setting.min() < 0) builder.append("-?");

        builder.append("\\d{0,");
        builder.append(String.valueOf(Math.max(Math.abs(this.setting.min()), Math.abs(this.setting.max()))).length());
        builder.append("}");
        builder.append("\\.?\\d{0,6}"); // up to 6 decimals

        String regex = builder.toString();
        return s -> s.matches(regex);
    }
}

