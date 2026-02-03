package com.pigicial.wikirenderer.components;

import com.pigicial.wikirenderer.property.IntProperty;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import io.wispforest.owo.ui.core.Sizing;

import java.util.Objects;
import java.util.function.Predicate;

public class IntegerPropertyTextFieldComponent extends TextBoxComponent {

    private final IntProperty setting;
    private String content = "";
    private boolean ignoringChange = false;

    private boolean previouslyFocused = false;

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

        this.setting.futureListen((integerSetting, integer) -> {
            if (!this.ignoringChange) {
                this.ignoringChange = true;
                this.text(String.valueOf(integer));
                this.ignoringChange = false;
            }
        });
    }

    @Override
    public void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        super.draw(graphics, mouseX, mouseY, partialTicks, delta);
        if (this.isFocused()) {
            this.previouslyFocused = true;
            return;
        }

        if (this.setting.hasRollover() && !this.isFocused() && previouslyFocused) {
            this.ignoringChange = true;
            this.text(String.valueOf(setting.get()));
            this.ignoringChange = false;
        }
    }

    private Predicate<String> makeMatcher() {
        StringBuilder builder = new StringBuilder();
        if (this.setting.min() < 0 || this.setting.hasRollover()) builder.append("-?");

        builder.append("\\d{0,");
        int maxNumberLength = String.valueOf(Math.max(Math.abs(this.setting.min()), Math.abs(this.setting.max()))).length();
        if (setting.hasRollover()) {
            maxNumberLength += 2; // probably enough extra
        }

        builder.append(maxNumberLength);
        builder.append("}");

        String regex = builder.toString();
        return s -> {
            boolean matches = s.matches(regex);
            if (matches && !this.setting.hasRollover() && !s.isEmpty() && !s.equals("-")) {
                int number = Integer.parseInt(s);
                return number >= this.setting.min() && number <= this.setting.max();
            }
            return matches;
        };
    }
}
