package com.pigicial.wikirenderer.components;

import io.wispforest.owo.ui.component.DropdownComponent;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SearchableEntityListComponent extends DropdownComponent {
    private final List<EntityType<?>> hiddenEntityTypes;
    private final Supplier<String> searchFilter;
    private final Supplier<List<Entity>> visibleEntitiesSupplier;

    public SearchableEntityListComponent(List<EntityType<?>> hiddenEntityTypes, Supplier<String> searchFilter, Supplier<List<Entity>> visibleEntitiesSupplier) {
        super(Sizing.content());
        this.visibleEntitiesSupplier = visibleEntitiesSupplier;
        this.closeWhenNotHovered(false);
        this.padding(Insets.of(5));
        this.surface(Surface.blur(10, 20));

        this.hiddenEntityTypes = hiddenEntityTypes;
        this.searchFilter = searchFilter;
    }

    @Override
    public void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        this.update();
        super.draw(graphics, mouseX, mouseY, partialTicks, delta);
    }

    public void update() {
        String filter = searchFilter.get();
        this.entries.clearChildren();

        List<Holder.Reference<EntityType<?>>> entityTypes = BuiltInRegistries.ENTITY_TYPE.listElements().toList();
        for (Holder.Reference<EntityType<?>> typeHolder : entityTypes) {
            EntityType<?> type = typeHolder.value();
            Component description = type.getDescription();
            String descriptionString = description.getString();
            if (descriptionString == null) continue;

            boolean allow = descriptionString.toLowerCase().contains(filter);
            if (filter.trim().equalsIgnoreCase("visible")) {
                allow = visibleEntitiesSupplier.get().stream().anyMatch(e -> e.getType() == type);
            }

            if (allow) {
                MutableComponent hideText = Component.literal("Hide " + descriptionString);
                boolean checked = hiddenEntityTypes.contains(type);

                this.entries.child(new LeftAlignedCheckbox(hideText, checked, pressed -> {
                    if (pressed) {
                        hiddenEntityTypes.add(type);
                    } else {
                        hiddenEntityTypes.remove(type);
                    }
                }));
            }
        }
    }

    public static class LeftAlignedCheckbox extends Button {

        protected boolean state;

        public LeftAlignedCheckbox(Component text, boolean state, Consumer<Boolean> onClick) {
            super(text, dropdownComponent -> {
            });

            this.state = state;
            this.onClick = dropdownComponent -> {
                this.state = !this.state;
                onClick.accept(this.state);
            };
            this.horizontalSizing(Sizing.content());
            this.horizontalTextAlignment(HorizontalAlignment.LEFT);
            this.margins(Insets.of(2, 2, 2, 2));
        }

        @Override
        public void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
            this.width -= 10;
            this.x += 12;
            super.draw(graphics, mouseX, mouseY, partialTicks, delta);
            this.x -= 12;
            this.width += 10;

            ParentUIComponent dropdown = this.parent;
            assert dropdown != null;
            int u = this.state ? 16 : 0;
            int v = 0;

            graphics.blit(RenderPipelines.GUI_TEXTURED, ICONS_TEXTURE,
                    dropdown.x() + dropdown.padding().get().left() + 1, y,
                    u, v,
                    9, 9,
                    32, 32
            );
        }

        @Override
        protected int determineHorizontalContentSize(Sizing sizing) {
            return super.determineHorizontalContentSize(sizing) + 12;
        }
    }

    public static class Button extends DropdownComponent.Button {

        public Button(Component text, Consumer<DropdownComponent> onClick) {
            super(null, text, onClick);
        }
    }

    public static class DynamicTextButton extends DropdownComponent.Button {

        private final Supplier<Component> textSupplier;

        public DynamicTextButton(Supplier<Component> textSupplier, Consumer<DropdownComponent> onClick) {
            super(null, textSupplier.get(), onClick);
            this.textSupplier = textSupplier;
        }

        @Override
        public void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
            this.text(textSupplier.get());
            this.applySizing();
            super.draw(graphics, mouseX, mouseY, partialTicks, delta);
        }
    }
}
