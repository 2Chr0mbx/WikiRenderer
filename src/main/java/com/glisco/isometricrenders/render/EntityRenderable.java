package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.property.IntProperty;
import com.glisco.isometricrenders.property.Property;
import com.glisco.isometricrenders.screen.IsometricUI;
import com.glisco.isometricrenders.screen.RenderScreen;
import com.glisco.isometricrenders.util.ExportPathSpec;
import com.glisco.isometricrenders.util.ParticleRestriction;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import io.wispforest.owo.ui.component.EntityComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import com.mojang.math.Axis;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class EntityRenderable extends DefaultRenderable<DefaultPropertyBundle> implements TickingRenderable<DefaultPropertyBundle> {

    private final Minecraft client = Minecraft.getInstance();
    private final Entity entity;

    public EntityRenderable(Entity entity) {
        this.entity = entity;
    }

    public static EntityRenderable of(EntityType<?> type, @Nullable CompoundTag nbt) {
        final var client = Minecraft.getInstance();

        if (nbt == null) {
            nbt = new CompoundTag();
        }

        nbt.putString("id", EntityType.getKey(type).toString());

        final var entity = EntityType.loadEntityRecursive(nbt, client.level, EntitySpawnReason.LOAD, EntityProcessor.NOP);
        entity.absSnapTo(client.player.getX(), client.player.getY(), client.player.getZ());

        return new EntityRenderable(entity);
    }

    public static EntityRenderable copyAsRenderable(Entity source) {
        return new EntityRenderable(copy(source));
    }

    public static Entity copy(Entity source) {
        if (source instanceof AbstractClientPlayer player) {
            return copyPlayer(player);
        }

        final var client = Minecraft.getInstance();

        var logging = new ProblemReporter.ScopedCollector(source.problemPath(), IsometricRenders.LOGGER);
        var view = TagValueOutput.createWithContext(logging, source.registryAccess());
        source.saveWithoutId(view);
        var nbt = view.buildResult();
        logging.close();
        nbt.putString("id", EntityType.getKey(source.getType()).toString());

        Entity entity = EntityType.loadEntityRecursive(nbt, client.level, EntitySpawnReason.LOAD, EntityProcessor.NOP);
        applyToEntityAndPassengers(entity, Entity::tick);

        return entity;
    }

    public static EntityComponent.RenderablePlayerEntity copyPlayer(AbstractClientPlayer originalPlayer) {
        GameProfile originalProfile = originalPlayer.getGameProfile();
        GameProfile fakeProfile = new GameProfile(originalProfile.id(), originalProfile.name(), new PropertyMap(originalProfile.properties()));

        final var player = EntityComponent.createRenderablePlayer(fakeProfile);

        ProblemReporter.ScopedCollector loggingWrite = new ProblemReporter.ScopedCollector(originalPlayer.problemPath(), IsometricRenders.LOGGER);
        var view = TagValueOutput.createWithContext(loggingWrite, originalPlayer.registryAccess());
        originalPlayer.saveWithoutId(view);
        var nbt = view.buildResult();
        loggingWrite.close();

        try (ProblemReporter.ScopedCollector loggingRead = new ProblemReporter.ScopedCollector(player.problemPath(), IsometricRenders.LOGGER)) {
            player.load(TagValueInput.create(loggingRead, player.registryAccess(), nbt));
        }

        return player;
    }

    @Override
    public void emitVertices(PoseStack matrices, MultiBufferSource vertexConsumers, float tickDelta) {
        matrices.pushPose();

        matrices.translate(0, -.5 * this.entity.getBbHeight(), 0);
        matrices.mulPose(Axis.YP.rotationDegrees(180));

        var properties = this.properties();
        this.entity.setYHeadRot(properties.yaw.get());
        if (entity instanceof LivingEntity living) living.yHeadRotO = properties.yaw.get();
        this.entity.yRotO = properties.yaw.get();

        this.entity.setXRot(properties.pitch.get());
        this.entity.xRotO = properties.pitch.get();

        final MutableObject<Vec3> offset = new MutableObject<>(Vec3.ZERO);

		final var renderDispatcher = client.getEntityRenderDispatcher();
		final var commandQueue = client.gameRenderer.getSubmitNodeStorage();
        applyToEntityAndPassengers(this.entity, entity -> {
            entity.setPosRaw(client.player.getX(), client.player.getY(), client.player.getZ());
            if (entity.isPassenger()) {
                offset.setValue(offset.getValue().add(entity.getVehicle().getPassengerRidingPosition(entity).subtract(entity.trackingPosition())));
            }

            var offsetPos = offset.getValue();
            matrices.pushPose();
			var state = renderDispatcher.extractEntity(entity, tickDelta);
			state.shadowPieces.clear(); // remove shadows
	        state.lightCoords = LightTexture.FULL_BRIGHT;

            // fix weird cape behavior with frozen models - there might be a better way to do this but ehh this is fine for now
            if (state instanceof AvatarRenderState avatarRenderState) {
                avatarRenderState.capeFlap = 0;
                avatarRenderState.capeLean = 0;
                avatarRenderState.capeLean2 = 0;

                if (properties.useSteveSkin.get()) {
                    avatarRenderState.skin = DefaultPlayerSkin.getDefaultSkin();
                }
            }

	        renderDispatcher.submit(state, new CameraRenderState(), offsetPos.x(), offsetPos.y(), offsetPos.z(), matrices, commandQueue);
            matrices.popPose();
        });

		client.gameRenderer.getFeatureRenderDispatcher().renderAllFeatures();

        matrices.mulPose(Axis.YP.rotationDegrees(-180));
        matrices.translate(0, 1.65, 0);

        this.renderParticles(matrices.last().pose(), tickDelta);

        matrices.popPose();
    }

    @Override
    public EntityPropertyBundle properties() {
        return EntityPropertyBundle.INSTANCE;
    }

    @Override
    public ParticleRestriction<?> particleRestriction() {
        return ParticleRestriction.duringTick();
    }

    @Override
    public ExportPathSpec exportPath() {
        return ExportPathSpec.ofIdentified(
                BuiltInRegistries.ENTITY_TYPE.getKey(this.entity.getType()),
                "entity"
        );
    }

    @Override
    public void tick() {
        applyToEntityAndPassengers(this.entity, entity -> {
            if (entity instanceof Player) return;
            client.level.tickNonPassenger(entity);
        });
    }

    private static void applyToEntityAndPassengers(Entity entity, Consumer<Entity> action) {
        action.accept(entity);
        if (entity.getPassengers().isEmpty()) return;
        for (Entity e : entity.getPassengers()) applyToEntityAndPassengers(e, action);
    }

    public static class EntityPropertyBundle extends DefaultPropertyBundle {

        public static final EntityPropertyBundle INSTANCE = new EntityPropertyBundle();

        public final IntProperty yaw = IntProperty.of(0, -180, 180).withRollover();
        public final IntProperty pitch = IntProperty.of(0, -90, 90).withRollover();
        public final Property<Boolean> useSteveSkin = Property.of(false);

        private EntityPropertyBundle() {}

        @Override
        public void buildGuiControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
            super.buildGuiControls(renderable, screen, container);

            IsometricUI.sectionHeader(container, "entity_data", true);

            IsometricUI.intControl(container, yaw, "entity_data.yaw", 15);
            IsometricUI.intControl(container, pitch, "entity_data.pitch", 5);
            if (renderable instanceof EntityRenderable entityRenderable && entityRenderable.entity instanceof Player) {
                IsometricUI.booleanControl(container, useSteveSkin, "entity_data.steve");
            }
        }
    }
}
