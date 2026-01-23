package com.glisco.isometricrenders.render.batch;

import com.glisco.isometricrenders.render.item.BlockStateRenderable;
import com.glisco.isometricrenders.render.item.ItemAtlasRenderable;
import com.glisco.isometricrenders.render.item.ItemRenderable;
import com.glisco.isometricrenders.render.item.TooltipRenderable;
import com.glisco.isometricrenders.screen.RenderScreen;
import com.glisco.isometricrenders.screen.ScreenSchedulerAndSaver;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiConsumer;

public enum BatchRenderTask {
    ATLAS((source, renderables) -> {
        ScreenSchedulerAndSaver.schedule(new RenderScreen(
                new ItemAtlasRenderable(source, new ArrayList<>(renderables))
        ));
    }),
    BATCH_ITEM((source, renderables) -> {
        ScreenSchedulerAndSaver.schedule(new RenderScreen(
                BatchRenderable.of(
                        source + "/items",
                        renderables.stream()
                                .map(ItemRenderable::new)
                                .toList()
                )
        ));
    }),
    BATCH_TOOLTIP((source, renderables) -> {
        ScreenSchedulerAndSaver.schedule(new RenderScreen(
                BatchRenderable.of(
                        source + "/tooltips",
                        renderables.stream()
                                .map(TooltipRenderable::new)
                                .toList()
                )
        ));
    }),
    BATCH_BLOCK((source, renderables) -> {
        ScreenSchedulerAndSaver.schedule(new RenderScreen(
                BatchRenderable.of(
                        source + "/blocks",
                        renderables.stream()
                                .filter(stack -> stack.getItem() instanceof BlockItem)
                                .map(stack -> ((BlockItem) stack.getItem()).getBlock())
                                .map(BlockStateRenderable::of)
                                .toList()
                )
        ));
    });

    public final BiConsumer<String, Collection<ItemStack>> action;

    BatchRenderTask(BiConsumer<String, Collection<ItemStack>> action) {
        this.action = action;
    }
}
