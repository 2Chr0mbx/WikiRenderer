package com.glisco.isometricrenders.textures;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.UUID;

public class SkinGrabber {

    protected static final Gson GSON = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).setPrettyPrinting().create();

    @Nullable
    public static MinecraftTexturesPayload getPlayerHeadTextureData(ItemStack itemStack) {
        ResolvableProfile profile = itemStack.get(DataComponents.PROFILE);
        if (profile == null) return null;

        return getGameProfileTextureData(profile.partialProfile());
    }

    @Nullable
    public static MinecraftTexturesPayload getGameProfileTextureData(GameProfile gameProfile) {
        Collection<Property> textures = gameProfile.properties().get("textures");
        if (textures.isEmpty()) {
            return null;
        }

        String skin = textures.iterator().next().value();
        byte[] byteArray;
        try {
            byteArray = Base64.getDecoder().decode(skin);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        String decodedSkin = new String(byteArray, StandardCharsets.UTF_8);

        return GSON.fromJson(decodedSkin, MinecraftTexturesPayload.class);
    }
}
