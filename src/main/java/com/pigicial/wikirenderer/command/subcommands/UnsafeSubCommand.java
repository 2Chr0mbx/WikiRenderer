package com.pigicial.wikirenderer.command.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.pigicial.wikirenderer.property.GlobalProperties;
import com.pigicial.wikirenderer.util.Translate;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class UnsafeSubCommand extends WikiRendererSubCommand {
    @Override
    public String getName() {
        return "unsafe";
    }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> register(LiteralArgumentBuilder<FabricClientCommandSource> source, CommandBuildContext access) {
        return source
                .then(literal("enable")
                        .executes(context -> {
                            this.enableUnsafe(context);
                            return 0;
                        }))
                .then(literal("disable")
                        .executes(context -> {
                            this.disableUnsafe(context);
                            return 0;
                        }));
    }


    private void disableUnsafe(CommandContext<FabricClientCommandSource> context) {
        GlobalProperties.UNSAFE.set(false);
        Translate.commandFeedback(context, "unsafe_disabled");
    }

    private void enableUnsafe(CommandContext<FabricClientCommandSource> context) {
        GlobalProperties.UNSAFE.set(true);
        Translate.commandFeedback(context, "unsafe_enabled");
    }
}
