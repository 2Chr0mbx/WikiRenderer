package com.pigicial.wikirenderer.components;

import com.pigicial.wikirenderer.render.entity.options.EntityTypeSpecificOverrides;
import com.pigicial.wikirenderer.render.entity.options.types.OptionalOverride;
import io.wispforest.owo.ui.component.DropdownComponent;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
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
        this.padding(Insets.of(5));
        this.surface(Surface.blur(10, 20));
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
            lastSavedEntity = entity;

            if (entity == null) return;

            EntityTypeSpecificOverrides<?> overrides = overridesSupplier.get();
            if (overrides == null) {
                return;
            }

            for (OptionalOverride<?, ?> override : overrides.getOverrides()) {
                this.entries.child(override.buildComponent());
            }
        }
    }
}
