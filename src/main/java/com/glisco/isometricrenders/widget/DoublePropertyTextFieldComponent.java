package com.glisco.isometricrenders.widget;

import com.glisco.isometricrenders.property.DoubleProperty;
import com.glisco.isometricrenders.property.IntProperty;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.text.NumberFormat;
import java.util.Objects;
import java.util.function.Predicate;

public class DoublePropertyTextFieldComponent extends TextBoxComponent {

    private final DoubleProperty setting;
    private String content = "";
    private boolean ignoringChange = false;

    public DoublePropertyTextFieldComponent(Sizing horizontalSizing, DoubleProperty setting) {
        super(horizontalSizing);
        this.setting = setting;

        this.text(String.format("%.1f", setting.get()));
        this.setFilter(makeMatcher());

        this.onChanged().subscribe(s -> {
            if (s.endsWith(".")) {
                return;
            }

            if (Objects.equals(s, content) || s.isEmpty() || s.equals("-")) {
                return;
            }

            this.content = s;
            if (!this.ignoringChange) {
                this.ignoringChange = true;
                this.setting.set(Double.parseDouble(s));
                this.ignoringChange = false;
            }
        });

        this.setting.listen((doubleSetting, value) -> {
            if (!this.ignoringChange) {
                this.ignoringChange = true;
                String number = String.format("%.1f", value);
                if (value.floatValue() == 35.264f) {
                    number = "35.264"; // jank but whatever
                }

                this.text(number.endsWith(".0") ? number.substring(0, number.length() - 2) : number);
                this.ignoringChange = false;
            }
        });
    }

    private Predicate<String> makeMatcher() {
        StringBuilder builder = new StringBuilder();
        if (this.setting.min() < 0) builder.append("-?");

        builder.append("\\d{0,");
        builder.append(String.valueOf(Math.max(Math.abs(this.setting.min()), Math.abs(this.setting.max()))).length());
        builder.append("}");
        builder.append("\\.?\\d{0,3}"); // up to 3 decimals

        String regex = builder.toString();
        return s -> {
            boolean matches = s.matches(regex);
            if (matches && !s.isEmpty() && !s.endsWith(".") && !s.equals("-")) {
                double number = Double.parseDouble(s);
                return number >= this.setting.min() && number <= this.setting.max();
            }

            return matches;
        };
    }
}

