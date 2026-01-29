package com.pigicial.wikirenderer.components;

import com.pigicial.wikirenderer.screen.WikiRendererUI;
import com.pigicial.wikirenderer.util.FileIO;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;

public class IOStateComponent extends FlowLayout {

    public IOStateComponent() {
        super(Sizing.content(), Sizing.content(), Algorithm.VERTICAL);

        this.padding(Insets.of(10));
        this.surface(Surface.flat(0x77000000).and(Surface.outline(0x77000000)));

        this.child(new DynamicLabelComponent(FileIO::progressText).margins(Insets.bottom(10)));
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        if (FileIO.taskCount() > 0) {
            super.draw(context, mouseX, mouseY, partialTicks, delta);
            WikiRendererUI.drawExportProgressBar(context, this.x + 10, this.y + 25, 100, 50, 10);
        }
    }
}
