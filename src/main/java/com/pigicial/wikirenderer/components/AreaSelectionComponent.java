package com.pigicial.wikirenderer.components;

import com.pigicial.wikirenderer.WikiRendererKeybinds;
import com.pigicial.wikirenderer.util.Translate;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

import static com.pigicial.wikirenderer.render.area.AreaSelectionHelper.pos1;
import static com.pigicial.wikirenderer.render.area.AreaSelectionHelper.pos2;

public class AreaSelectionComponent extends FlowLayout {

    public AreaSelectionComponent() {
        super(Sizing.content(), Sizing.content(), Algorithm.VERTICAL);

        this.surface(Surface.flat(0x77000000).and(Surface.outline(0x77000000)));
        this.padding(Insets.of(5));

        this.child(UIComponents.label(Translate.PREFIX).shadow(true).margins(Insets.bottom(10)));

        this.child(UIComponents.label(Translate.gui("hud.area_selection")));
        this.child(new DynamicLabelComponent(
                positionText(() -> pos1, "from"))
                .shadow(false).horizontalSizing(Sizing.fixed(Minecraft.getInstance().font.width(positionText(() -> pos1, "from").get())))
        );
        this.child(new DynamicLabelComponent(
                positionText(() -> pos2, "to"))
                .shadow(false).margins(Insets.bottom(10))
        );

        Component firstKeybind = Component.keybind(WikiRendererKeybinds.KEYBIND_SELECT_AREA.getName()).withStyle(ChatFormatting.YELLOW);
        Component secondKeybind = Component.keybind(WikiRendererKeybinds.KEYBIND_SELECT_AREA_EXPAND.getName()).withStyle(ChatFormatting.YELLOW);
        this.child(UIComponents.label(Translate.gui("hud.area_selection.clear_hint", firstKeybind, secondKeybind)));
    }

    private Supplier<Component> positionText(Supplier<BlockPos> pos, String name) {
        return () -> {
            Component positionText = Component.literal(pos.get() == null ? "---" : pos.get().getX() + " " + pos.get().getY() + " " + pos.get().getZ()).withStyle(ChatFormatting.GRAY);
            return Translate.gui("hud.area_selection." + name, positionText);
        };
    }

}
