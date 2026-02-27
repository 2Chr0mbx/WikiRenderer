package com.pigicial.wikirenderer.command.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.pigicial.wikirenderer.property.GlobalProperties;
import com.pigicial.wikirenderer.util.Translate;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class DebugSubCommand extends WikiRendererSubCommand {
    @Override
    public String getName() {
        return "debug";
    }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> register(LiteralArgumentBuilder<FabricClientCommandSource> source, CommandBuildContext access) {
        return source
                .then(literal("entity_area_bounds")
                        .then(literal("enable")
                                .executes(context -> {
                                    this.enableDebugBounds(context);
                                    return 0;
                                }))
                        .then(literal("disable")
                                .executes(context -> {
                                    this.disableDebugBounds(context);
                                    return 0;
                                })))
                .then(literal("unsafe")
                        .then(literal("enable")
                                .executes(context -> {
                                    this.enableUnsafe(context);
                                    return 0;
                                }))
                        .then(literal("disable")
                                .executes(context -> {
                                    this.disableUnsafe(context);
                                    return 0;
                                })));
    }

    private void enableDebugBounds(CommandContext<FabricClientCommandSource> context) {
        GlobalProperties.DEBUG_SHOW_COLLIDING_ENTITY_BOUNDS_FOR_AREAS.set(true);
        Translate.commandFeedback(context, "debug_bounds_enabled");
    }

    private void disableDebugBounds(CommandContext<FabricClientCommandSource> context) {
        GlobalProperties.DEBUG_SHOW_COLLIDING_ENTITY_BOUNDS_FOR_AREAS.set(false);
        Translate.commandFeedback(context, "debug_bounds_disabled");
    }

    private void enableUnsafe(CommandContext<FabricClientCommandSource> context) {
        GlobalProperties.UNSAFE.set(true);
        Translate.commandFeedback(context, "unsafe_enabled");
    }

    private void disableUnsafe(CommandContext<FabricClientCommandSource> context) {
        GlobalProperties.UNSAFE.set(false);
        Translate.commandFeedback(context, "unsafe_disabled");
    }
}

