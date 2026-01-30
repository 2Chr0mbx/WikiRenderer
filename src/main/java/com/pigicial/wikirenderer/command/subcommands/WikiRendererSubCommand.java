package com.pigicial.wikirenderer.command.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;

public abstract class WikiRendererSubCommand {
    public abstract String getName();

    public abstract LiteralArgumentBuilder<FabricClientCommandSource> register(LiteralArgumentBuilder<FabricClientCommandSource> source, CommandBuildContext access);
}
