package com.pigicial.wikirenderer.textures;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;

public record TextureData(MinecraftTexturesPayload payload, GameProfile profile) {
}
