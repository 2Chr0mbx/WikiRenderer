package com.pigicial.wikirenderer.components;

import com.pigicial.wikirenderer.property.IntProperty;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.core.Sizing;

import java.util.Objects;
import java.util.function.Predicate;

public class IntegerPropertyTextFieldComponent extends TextBoxComponent {

    private final IntProperty setting;
    private String content = "";
    private boolean ignoringChange = false;

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

            if (!this.ignoringChange) {
                this.ignoringChange = true;
                this.setting.set(Integer.parseInt(s));
                this.ignoringChange = false;
            }
        });

        this.setting.listen((integerSetting, integer) -> {
            if (!this.ignoringChange) {
                this.ignoringChange = true;
                this.text(String.valueOf(integer));
                this.ignoringChange = false;
            }
        }, false);
    }

    private Predicate<String> makeMatcher() {
        StringBuilder builder = new StringBuilder();
        if (this.setting.min() < 0) builder.append("-?");

        builder.append("\\d{0,");
        builder.append(String.valueOf(Math.max(Math.abs(this.setting.min()), Math.abs(this.setting.max()))).length());
        builder.append("}");

        String regex = builder.toString();
        return s -> {
            boolean matches = s.matches(regex);
            if (matches && !s.isEmpty() && !s.equals("-")) {
                int number = Integer.parseInt(s);
                return number >= this.setting.min() && number <= this.setting.max();
            }
            return matches;
        };
    }
}
