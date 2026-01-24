package com.glisco.isometricrenders.widget;

import com.glisco.isometricrenders.property.IntProperty;
import com.glisco.isometricrenders.property.NumberProperty;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.function.Predicate;

public class IntegerPropertyTextFieldComponent extends TextBoxComponent {

    private final IntProperty setting;
    private String content = "";

    public IntegerPropertyTextFieldComponent(Sizing horizontalSizing, IntProperty setting) {
        super(horizontalSizing);
        this.setting = setting;

        String value = String.valueOf(setting.get());
        this.text(value);
        this.setFilter(makeMatcher());

        this.onChanged().subscribe(s -> {
            if (Objects.equals(s, content) || s.isEmpty() || s.equals("-")) {
                return;
            }

            this.content = s;
            this.setting.set(Integer.parseInt(s));
        });

        this.setting.listen((integerSetting, integer) -> {
            this.setValue(String.valueOf(integer));
        }, false);
    }

    private Predicate<String> makeMatcher() {
        StringBuilder builder = new StringBuilder();
        if (this.setting.min() < 0) builder.append("-?");

        builder.append("\\d{0,");
        builder.append(String.valueOf(Math.max(Math.abs(this.setting.min()), Math.abs(this.setting.max()))).length());
        builder.append("}");

        String regex = builder.toString();
        return s -> s.matches(regex);
    }
}
