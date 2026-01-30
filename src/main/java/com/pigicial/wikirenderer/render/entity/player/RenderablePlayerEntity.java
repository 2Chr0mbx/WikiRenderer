package com.pigicial.wikirenderer.render.entity.player;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.telemetry.TelemetryEventSender;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.stats.StatsCounter;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.player.PlayerSkin;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public class RenderablePlayerEntity extends LocalPlayer {

    protected PlayerSkin skinTextures;

    public RenderablePlayerEntity(GameProfile profile, ProfileFetchMode fetchMode) {
        super(Minecraft.getInstance(),
                Objects.requireNonNull(Minecraft.getInstance().level),
                new ClientPacketListener(Minecraft.getInstance(),
                        new net.minecraft.network.Connection(PacketFlow.CLIENTBOUND),
                        new CommonListenerCookie(
                                new LevelLoadTracker(0),
                                profile, new WorldSessionTelemetryManager(TelemetryEventSender.DISABLED, false, Duration.ZERO, ""),
                                Minecraft.getInstance().level.registryAccess().freeze(),
                                Minecraft.getInstance().level.enabledFeatures(),
                                "Wisp Forest Enterprises", null, null, Map.of(), null, Map.of(), ServerLinks.EMPTY, Map.of(),
                                true
                        )),
                new StatsCounter(), new ClientRecipeBook(), Input.EMPTY, false
        );

        this.skinTextures = DefaultPlayerSkin.get(profile);
        Util.backgroundExecutor().execute(() -> {
            ProfileResolver profileResolver = Minecraft.getInstance().services().profileResolver();
            GameProfile completeProfile = switch (fetchMode) {
                case NAME -> profileResolver.fetchByName(profile.name()).orElse(profile);
                case UUID -> profileResolver.fetchById(profile.id()).orElse(profile);
                case TEXTURE -> profile;
            };

            this.skinTextures = DefaultPlayerSkin.get(completeProfile);
            this.minecraft.getSkinManager().get(completeProfile).thenAccept(textures -> textures.ifPresent(skin -> this.skinTextures = skin));
        });
    }

    @Override
    public @NonNull PlayerSkin getSkin() {
        return this.skinTextures;
    }

    @Override
    public boolean isModelPartShown(@NonNull PlayerModelPart part) {
        return true;
    }

    @Nullable
    @Override
    protected PlayerInfo getPlayerInfo() {
        return null;
    }
}