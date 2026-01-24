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

        this.text(String.valueOf(setting.get()));
        this.setFilter(makeMatcher());

        this.onChanged().subscribe(s -> {
            if (s.endsWith(".")) {
                return;
            }

            if (Objects.equals(s, content) || s.isEmpty() || s.equals("-")) {
                return;
            }

            this.content = s;
            this.ignoringChange = true;
            this.setting.set(Double.parseDouble(s));
            this.ignoringChange = false;
            this.text(s);
        });

        this.setting.listen((doubleSetting, value) -> {
            if (!this.ignoringChange) {
                this.text(String.valueOf(value));
            }
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

