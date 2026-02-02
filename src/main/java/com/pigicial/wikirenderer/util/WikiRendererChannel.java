package com.pigicial.wikirenderer.util;

import com.pigicial.wikirenderer.render.item.ItemRenderable;
import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.screen.RenderScreen;
import io.wispforest.exo.api.Exo;
import io.wispforest.exo.api.ExoCommandChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class WikiRendererChannel extends ExoCommandChannel {

    public WikiRendererChannel() {
        addCommand("open-screen", (port, arguments) -> {
            Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new RenderScreen(Renderable.EMPTY)));
            return Exo.OK_RESPONSE;
        });

        addCommand("render-item", (port, arguments) -> {
            if (arguments.length < 1) return Exo.join("error", "missing item id");

            Identifier id = Identifier.parse(arguments[0]);
            Minecraft client = Minecraft.getInstance();

            if (BuiltInRegistries.ITEM.containsKey(id)) {
                Minecraft.getInstance().execute(() -> {
                    ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(id));
                    ItemRenderable renderable = new ItemRenderable(stack);

                    RenderScreen screen = new RenderScreen(renderable);

                    screen.setExportCallback(file -> send(Exo.join("exported", id.toString(), file.getAbsolutePath()), port));
                    screen.scheduleCapture();
                    client.setScreen(screen);
                });
                return Exo.OK_RESPONSE;
            } else {
                return Exo.join("error", "unknown item");
            }
        });
    }

    @Override
    public String getId() {
        return "isometric";
    }
}
