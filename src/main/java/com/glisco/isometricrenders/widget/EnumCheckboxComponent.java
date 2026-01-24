package com.glisco.isometricrenders.widget;

import com.glisco.isometricrenders.mixin.access.CheckBoxAccessor;
import com.glisco.isometricrenders.property.Property;
import io.wispforest.owo.ui.component.CheckboxComponent;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

public class EnumCheckboxComponent<T extends Enum<T>> extends CheckboxComponent {

    private final Property<T> property;
    private final T desired;
    private final T fallback;
    private boolean ignoringListener = false;

    public EnumCheckboxComponent(Component message, Property<T> property, T desired, T fallback) {
        super(message);

        this.property = property;
        this.desired = desired;
        this.fallback = fallback;
        this.checked(this.property.get() == desired);

        this.property.listen((p, value) -> {
            if (!this.ignoringListener) {
                ((CheckBoxAccessor) this).isometric$setSelected(value == desired);
            }
        }, false);
    }

    @Override
    public void onPress(InputWithModifiers input) {
        super.onPress(input);
        this.ignoringListener = true;
        if (this.selected()) {
            this.property.set(this.desired);
        } else {
            this.property.set(this.fallback);
        }
        this.ignoringListener = false;
    }
}
