package com.pigicial.wikirenderer.components;

import com.pigicial.wikirenderer.render.entity.options.EntityTypeSpecificOverrides;
import com.pigicial.wikirenderer.render.entity.options.types.OptionalOverride;
import com.pigicial.wikirenderer.util.Translate;
import io.wispforest.owo.ui.component.DropdownComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.core.*;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.Entity;

import java.util.function.Supplier;

public class EntityTypeSpecificPropertiesComponent extends DropdownComponent {

    private final Supplier<Entity> entitySupplier;
    private final Supplier<EntityTypeSpecificOverrides<?>> overridesSupplier;

    private Entity lastSavedEntity = null;

    public EntityTypeSpecificPropertiesComponent(Supplier<Entity> entitySupplier, Supplier<EntityTypeSpecificOverrides<?>> overridesSupplier) {
        super(Sizing.content());
        this.entitySupplier = entitySupplier;
        this.overridesSupplier = overridesSupplier;

        this.closeWhenNotHovered(false);
        this.padding(Insets.of(7, 0, 0, 5));
        this.surface(Surface.blur(10, 10));
    }

    @Override
    public void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        this.update();
        super.draw(graphics, mouseX, mouseY, partialTicks, delta);
    }

    public void update() {
        Entity entity = entitySupplier.get();
        if (entity != lastSavedEntity) {
            this.entries.clearChildren();
            this.lastSavedEntity = entity;

            if (entity == null) return;

            EntityTypeSpecificOverrides<?> overrides = this.overridesSupplier.get();
            if (overrides == null) {
                return;
            }

            if (!overrides.getOverrides().isEmpty()) {
                this.text(Translate.gui("advanced_entity_data").withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE));

                LabelComponent label = new AutoResizingLabelComponent(Translate.gui("advanced_entity_data_notice"));
                label.color(Color.ofFormatting(ChatFormatting.GRAY));
                label.margins(Insets.of(2));
                this.entries.child(label);

                for (OptionalOverride<?, ?> override : overrides.getOverrides()) {
                    this.entries.child(override.buildComponent());
                }
            }
        }
    }
}
