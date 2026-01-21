package com.glisco.isometricrenders.util;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;

public class RenderTargetUtils {
	public static GpuTexture cloneColorAttachment(RenderTarget renderTarget) {
		GpuTexture original = renderTarget.getColorTexture();
		assert original != null;

		GpuTexture copy = RenderSystem.getDevice().createTexture(() -> "[IsometricRenders] Copy of: " + original.getLabel(),
				GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
				TextureFormat.RGBA8, renderTarget.width, renderTarget.height, 1, 1);

		RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(original, copy, 0, 0, 0, 0, 0, renderTarget.width, renderTarget.height);

		return copy;
	}
}
