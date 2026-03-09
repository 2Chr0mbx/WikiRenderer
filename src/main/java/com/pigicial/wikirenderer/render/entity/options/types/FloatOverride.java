package com.pigicial.wikirenderer.render.entity.options.types;

import com.pigicial.wikirenderer.components.MiniEditBoxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class FloatOverride<S extends EntityRenderState>  extends OptionalOverride<S, Float> {

    public FloatOverride(String key, Function<S, Float> getter, BiConsumer<S, Float> setter) {
        super(key, getter, setter);
    }

    @Override
    protected void addToComponentRow(@UnknownNullability FlowLayout row) {
        MiniEditBoxComponent editBox = new MiniEditBoxComponent(Sizing.fixed(50), Float.toString(this.getValue()));
        editBox.setFilter(this::isValidNumber);
        editBox.onChanged().subscribe(text -> this.setValue(text.isBlank() ? 0 : Float.parseFloat(text.trim())));
        editBox.focusLost().subscribe(() -> editBox.text(Float.toString(this.getValue())));

        row.child(editBox);
    }

    private boolean isValidNumber(String s) {
        if (s.isBlank()) {
            return true;
        }
        try {
            Float.parseFloat(s.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Float getDefaultValue() {
        return 0f;
    }
}
