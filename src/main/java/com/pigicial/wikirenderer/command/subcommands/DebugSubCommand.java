package com.pigicial.wikirenderer.command.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.pigicial.wikirenderer.property.GlobalProperties;
import com.pigicial.wikirenderer.util.Translate;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.entity.animal.chicken.ChickenVariant;
import net.minecraft.world.entity.animal.chicken.ChickenVariants;

import java.util.List;
import java.util.Optional;

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
        GlobalProperties.get().debugShowCollidingEntityBoundsForAreas.set(true);
        Translate.commandFeedback(context, "debug_bounds_enabled");
    }

    private void disableDebugBounds(CommandContext<FabricClientCommandSource> context) {
        GlobalProperties.get().debugShowCollidingEntityBoundsForAreas.set(false);
        Translate.commandFeedback(context, "debug_bounds_disabled");
    }

    private void enableUnsafe(CommandContext<FabricClientCommandSource> context) {
        GlobalProperties.get().unsafe.set(true);
        Translate.commandFeedback(context, "unsafe_enabled");
    }

    private void disableUnsafe(CommandContext<FabricClientCommandSource> context) {
        GlobalProperties.get().unsafe.set(false);
        Translate.commandFeedback(context, "unsafe_disabled");

        Optional<? extends HolderLookup.RegistryLookup<ChickenVariant>> lookup = VanillaRegistries.createLookup().lookup(Registries.CHICKEN_VARIANT);
        if (lookup.isPresent()) {
            HolderLookup.RegistryLookup<ChickenVariant> registryLookup = lookup.get();
            List<Holder.Reference<ChickenVariant>> list = registryLookup.listElements().toList();
            for (Holder.Reference<ChickenVariant> chickenVariantReference : list) {
                System.out.println("chickenVariantReference.value() = " + chickenVariantReference.value());
            }
        }

    }
}

