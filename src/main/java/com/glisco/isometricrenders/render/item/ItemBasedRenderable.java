package com.glisco.isometricrenders.render.item;

import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.render.DefaultRenderable;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

public abstract class ItemBasedRenderable<T extends DefaultPropertyBundle> extends DefaultRenderable<T> {

    protected void setupLighting(ItemStackRenderState itemRenderState) {
        setupLighting(itemRenderState.usesBlockLight());
    }

    protected void setupLighting(boolean usesBlockLight) {
        if (usesBlockLight) {

            // pulled from Lighting's first setup for ITEMS_3D, but with the scaling value of y changed from -1.0f to 1.0f
            // ngl i have absolutely no idea why this works, but it does - it might have something to do with their atlas sheets having an inverted Y value, idk, probably does
            // (for that, see CachedOrthoProjectionMatrixBuffer and how when it's created in GuiRendered, flip y is true)
            // also i changed the numbers here to use Math.toRadians() rather than harder to process numbers
            Matrix4f matrix4f2 = new Matrix4f()
                    .scaling(1.0F, 1.0F, 1.0F) // IMPORTANT: the y is changed from -1.0 to 1.08
                    .rotateYXZ((float) Math.toRadians(62), (float) Math.toRadians(185.5), 0.0F)
                    .rotateYXZ((float) Math.toRadians(-22.5), (float) (Math.toRadians(135)), 0.0F);

            Vector3f light0 = matrix4f2.transformDirection(new Vector3f(0.2F, 1.0F, -0.7F).normalize(), new Vector3f());
            Vector3f light1 = matrix4f2.transformDirection(new Vector3f(-0.2F, 1.0F, 0.7F).normalize(), new Vector3f());

            this.setupLighting(light0, light1);
        } else {
            Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_FLAT);
        }
    }
}
