package com.pigicial.wikirenderer.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pigicial.wikirenderer.mixin.access.LevelRendererAccessor;
import com.pigicial.wikirenderer.property.DefaultPropertyBundle;
import com.pigicial.wikirenderer.render.CameraOrientationUtil;
import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.util.VertexPositionTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.TextDisplayEntityRenderState;
import net.minecraft.client.renderer.feature.*;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EntityRenderBoundsUtil {

    private static final VertexPositionTracker.BufferSource BUFFER_SOURCE = new VertexPositionTracker.BufferSource();
    private static final VertexPositionTracker.OutlineBufferSource OUTLINE_BUFFER_SOURCE = new VertexPositionTracker.OutlineBufferSource();

    private static final ModelFeatureRenderer MODEL_FEATURE_RENDERER = new ModelFeatureRenderer();
    private static final ModelPartFeatureRenderer MODEL_PART_FEATURE_RENDERER = new ModelPartFeatureRenderer();
    private static final FlameFeatureRenderer FLAME_FEATURE_RENDERER = new FlameFeatureRenderer();
    private static final NameTagFeatureRenderer NAME_TAG_FEATURE_RENDERER = new NameTagFeatureRenderer();
    private static final TextFeatureRenderer TEXT_FEATURE_RENDERER = new TextFeatureRenderer();
    private static final ItemFeatureRenderer ITEM_FEATURE_RENDERER = new ItemFeatureRenderer();
    private static final BlockFeatureRenderer BLOCK_FEATURE_RENDERER = new BlockFeatureRenderer();
    private static final CustomFeatureRenderer CUSTOM_FEATURE_RENDERER = new CustomFeatureRenderer();

    @Nullable
    public static AABB getPositionOffsetBasedBounds(Entity entity) {
        EntityRenderState entityRenderState = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(entity, 0);
        CameraRenderState cameraRenderState = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).wikirenderer$getLevelRenderState().cameraRenderState;
        Vec3 position = entity.position();
        return getBounds(entityRenderState, cameraRenderState, position.x, position.y, position.z);
    }

    @Nullable
    public static AABB getBounds(EntityRenderState renderState, Renderable<? extends DefaultPropertyBundle> renderable, double xOffset, double yOffset, double zOffset) {
        return getBounds(renderState, CameraOrientationUtil.createRenderState(renderable), xOffset, yOffset, zOffset);
    }

    // this gets the actual bounds of the rendered entity, rather than relying on extremely flaky and inconsistent bounding box data
    @Nullable
    public static AABB getBounds(EntityRenderState renderState, CameraRenderState cameraRenderState, double xOffset, double yOffset, double zOffset) {
        SubmitNodeStorage tempStorage = new SubmitNodeStorage();
        Minecraft.getInstance().getEntityRenderDispatcher().submit(renderState, cameraRenderState, xOffset, yOffset, zOffset, new PoseStack(), tempStorage);

        VertexPositionTracker.BOUNDS = null;
        for (SubmitNodeCollection collection : tempStorage.getSubmitsPerOrder().values()) {
            MODEL_FEATURE_RENDERER.render(collection, BUFFER_SOURCE, OUTLINE_BUFFER_SOURCE, BUFFER_SOURCE);
            MODEL_PART_FEATURE_RENDERER.render(collection, BUFFER_SOURCE, OUTLINE_BUFFER_SOURCE, BUFFER_SOURCE);
            FLAME_FEATURE_RENDERER.render(collection, BUFFER_SOURCE, Minecraft.getInstance().getAtlasManager());
            NAME_TAG_FEATURE_RENDERER.render(collection, BUFFER_SOURCE, Minecraft.getInstance().font);
            TEXT_FEATURE_RENDERER.render(collection, BUFFER_SOURCE);
            ITEM_FEATURE_RENDERER.render(collection, BUFFER_SOURCE, OUTLINE_BUFFER_SOURCE);
            BLOCK_FEATURE_RENDERER.render(collection, BUFFER_SOURCE, Minecraft.getInstance().getBlockRenderer(), OUTLINE_BUFFER_SOURCE);
            CUSTOM_FEATURE_RENDERER.render(collection, BUFFER_SOURCE);
        }

        return VertexPositionTracker.BOUNDS;
    }
}
