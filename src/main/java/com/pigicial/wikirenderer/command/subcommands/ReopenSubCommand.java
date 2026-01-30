package com.pigicial.wikirenderer.command.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.pigicial.wikirenderer.screen.ScreenSchedulerAndSaver;
import com.pigicial.wikirenderer.util.Translate;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;

public class ReopenSubCommand extends WikiRendererSubCommand {

    @Override
    public String getName() {
        return "reopen";
    }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> register(LiteralArgumentBuilder<FabricClientCommandSource> source, CommandBuildContext access) {
        return source.executes(context -> {
            reopenSavedMenu(context);
            return 0;
        });
    }

    private void reopenSavedMenu(CommandContext<FabricClientCommandSource> context) {
        if (ScreenSchedulerAndSaver.getSavedScreen() == null) {
            Translate.commandFeedback(context, "no_saved_menu");
        } else {
            ScreenSchedulerAndSaver.schedule(ScreenSchedulerAndSaver.getSavedScreen());
        }
    }
}
