package com.pigicial.wikirenderer.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pigicial.wikirenderer.property.DefaultPropertyBundle;
import com.pigicial.wikirenderer.render.CameraOrientationUtil;
import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.util.VertexPositionTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.*;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class EntityRenderBoundsUtil {

    private static final VertexPositionTracker.BufferSource BUFFER_SOURCE = new VertexPositionTracker.BufferSource();
    private static final VertexPositionTracker.OutlineBufferSource OUTLINE_BUFFER_SOURCE = new VertexPositionTracker.OutlineBufferSource();

    // this gets the actual bounds of the rendered entity, rather than relying on extremely flaky and inconsistent bounding box data
    @Nullable
    public static AABB getBounds(EntityRenderState renderState, Renderable<? extends DefaultPropertyBundle> renderable, double xOffset, double yOffset, double zOffset) {
        SubmitNodeStorage tempStorage = new SubmitNodeStorage();
        Minecraft.getInstance().getEntityRenderDispatcher().submit(renderState, CameraOrientationUtil.createRenderState(renderable), xOffset, yOffset, zOffset, new PoseStack(), tempStorage);

        VertexPositionTracker.BOUNDS = null;
        for (SubmitNodeCollection collection : tempStorage.getSubmitsPerOrder().values()) {
            new ModelFeatureRenderer().render(collection, BUFFER_SOURCE, OUTLINE_BUFFER_SOURCE, BUFFER_SOURCE);
            new ModelPartFeatureRenderer().render(collection, BUFFER_SOURCE, OUTLINE_BUFFER_SOURCE, BUFFER_SOURCE);
            new FlameFeatureRenderer().render(collection, BUFFER_SOURCE, Minecraft.getInstance().getAtlasManager());
            new TextFeatureRenderer().render(collection, BUFFER_SOURCE);
            new ItemFeatureRenderer().render(collection, BUFFER_SOURCE, OUTLINE_BUFFER_SOURCE);
            new BlockFeatureRenderer().render(collection, BUFFER_SOURCE, Minecraft.getInstance().getBlockRenderer(), OUTLINE_BUFFER_SOURCE);
            new CustomFeatureRenderer().render(collection, BUFFER_SOURCE);
        }

        return VertexPositionTracker.BOUNDS;
    }
}
