package com.pigicial.wikirenderer.render.entity.options.types;

import com.pigicial.wikirenderer.components.MiniEditBoxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class IntegerOverride<S extends EntityRenderState> extends OptionalOverride<S, Integer> {

    public IntegerOverride(String key, Function<S, Integer> getter, BiConsumer<S, Integer> setter) {
        super(key, getter, setter);
    }

    @Override
    protected void addToComponentRow(@UnknownNullability FlowLayout row) {
        MiniEditBoxComponent editBox = new MiniEditBoxComponent(Sizing.fixed(50), Integer.toString(this.getValue()));
        editBox.setFilter(this::isValidNumber);
        editBox.onChanged().subscribe(text -> this.setValue(text.isBlank() ? 0 : Integer.parseInt(text.trim())));
        editBox.focusLost().subscribe(() -> editBox.text(Integer.toString(this.getValue())));

        row.child(editBox);
    }

    private boolean isValidNumber(String s) {
        if (s.isBlank()) {
            return true;
        }
        try {
            Integer.parseInt(s.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Integer getDefaultValue() {
        return 0;
    }
}
