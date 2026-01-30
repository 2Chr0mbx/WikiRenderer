package com.pigicial.wikirenderer.command.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.pigicial.wikirenderer.render.item.ItemRenderable;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.screen.ScreenSchedulerAndSaver;
import com.pigicial.wikirenderer.util.Translate;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.world.item.ItemStack;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class RenderItemSubCommand extends WikiRendererSubCommand {
    @Override
    public String getName() {
        return "item";
    }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> register(LiteralArgumentBuilder<FabricClientCommandSource> source, CommandBuildContext access) {
        return source.executes(context -> {
                    this.renderHeldItem(context);
                    return 0;
                })
                .then(argument("item", ItemArgument.item(access))
                        .executes(context -> {
                            renderItemWithArgument(context);
                            return 0;
                        }));
    }

    private void renderItemWithArgument(CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException {
        ScreenSchedulerAndSaver.schedule(new RenderScreen(
                new ItemRenderable(ItemArgument.getItem(context, "item").createItemStack(1, false))
        ));
    }

    private void renderHeldItem(CommandContext<FabricClientCommandSource> context) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack mainHandItem = player.getMainHandItem();
        if (mainHandItem.isEmpty()) {
            Translate.commandError(context, "no_held_item");
            return;
        }

        ScreenSchedulerAndSaver.schedule(new RenderScreen(new ItemRenderable(mainHandItem)));
    }
}
