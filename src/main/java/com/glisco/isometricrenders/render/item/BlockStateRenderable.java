package com.glisco.isometricrenders.render.item;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.mixin.access.BlockEntityAccessor;
import com.glisco.isometricrenders.property.DefaultCroppablePropertyBundle;
import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.render.DefaultRenderable;
import com.glisco.isometricrenders.render.TickingRenderable;
import com.glisco.isometricrenders.util.ExportPathSpec;
import com.glisco.isometricrenders.util.ParticleRestriction;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;

public class BlockStateRenderable extends DefaultRenderable<DefaultCroppablePropertyBundle> implements TickingRenderable<DefaultCroppablePropertyBundle> {

    public static final DefaultCroppablePropertyBundle PROPERTIES = new DefaultCroppablePropertyBundle();

    private final Minecraft client = Minecraft.getInstance();

    private final BlockState state;
    private final @Nullable BlockEntity entity;

    public BlockStateRenderable(BlockState state, @Nullable BlockEntity entity) {
        this.state = state;
        this.entity = entity;
    }

    public static BlockStateRenderable of(Block block) {
        return of(block.defaultBlockState(), null);
    }

    public static BlockStateRenderable of(BlockState state, @Nullable CompoundTag nbt) {
        Minecraft client = Minecraft.getInstance();

        BlockEntity blockEntity = null;

        if (state.getBlock() instanceof EntityBlock provider) {
            blockEntity = provider.newBlockEntity(client.player.blockPosition(), state);
            prepareBlockEntity(state, blockEntity, nbt);
        }

        return new BlockStateRenderable(state, blockEntity);
    }

    public static BlockStateRenderable copyOf(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        CompoundTag data = world.getBlockEntity(pos) != null
                ? world.getBlockEntity(pos).saveWithoutMetadata(world.registryAccess())
                : null;

        return of(state, data);
    }

    @Override
    public void emitVerticesThenDraw(Matrix4fStack matrix4fStack, PoseStack matrices, MultiBufferSource vertexConsumers, float tickDelta) {
        matrices.pushPose();
        matrices.translate(-0.5, -0.5, -0.5);

		BlockEntityRenderState renderState = this.entity == null ? null : this.client.getBlockEntityRenderDispatcher().tryExtractRenderState(entity, tickDelta, null);

		if (renderState != null) {
			renderState.lightCoords = LightTexture.FULL_BRIGHT;
			this.client.getBlockEntityRenderDispatcher().submit(renderState, matrices, this.client.gameRenderer.getSubmitNodeStorage(), new CameraRenderState());
        } else if (this.state.getRenderShape() != RenderShape.INVISIBLE) {
	        this.client.getBlockRenderer().renderSingleBlock(this.state, matrices, vertexConsumers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        }

		super.drawSubmittedRenderFeatures();

        double xOffset = this.client.player.getX() % 1d;
        double zOffset = this.client.player.getZ() % 1d;

        if (xOffset < 0) xOffset += 1;
        if (zOffset < 0) zOffset += 1;

        matrices.translate(xOffset, 1.65 + this.client.player.getY() % 1d, zOffset);
        this.drawParticles(matrices.last().pose(), tickDelta);

        matrices.popPose();
    }

    @Override
    public void tick() {
        if (this.entity != null && this.state.getTicker(client.level, this.entity.getType()) != null) {
            BlockEntityTicker<BlockEntity> ticker = this.state.getTicker(client.level, (BlockEntityType<BlockEntity>) this.entity.getType());
            if (ticker == null) return;

            ticker.tick(client.level, client.player.blockPosition(), this.state, this.entity);
        }

        if (client.level.random.nextDouble() < 0.150) {
            this.state.getBlock().animateTick(this.state, client.level, client.player.blockPosition(), client.level.random);
        }
    }

    @Override
    public DefaultCroppablePropertyBundle getProperties() {
        return PROPERTIES;
    }

    @Override
    public ParticleRestriction<?> getParticleRestriction() {
        return ParticleRestriction.duringTick();
    }

    @Override
    public ExportPathSpec getExportPath() {
        return ExportPathSpec.ofIdentified(
                BuiltInRegistries.BLOCK.getKey(this.state.getBlock()),
                "block"
        );
    }

    private static void prepareBlockEntity(BlockState state, BlockEntity blockEntity, @Nullable CompoundTag nbt) {
        if (blockEntity == null) return;

        ((BlockEntityAccessor) blockEntity).isometric$setBlockState(state);
        blockEntity.setLevel(Minecraft.getInstance().level);

        if (nbt == null) return;

        CompoundTag nbtCopy = nbt.copy();

        nbtCopy.putInt("x", 0);
        nbtCopy.putInt("y", 0);
        nbtCopy.putInt("z", 0);
		try (ProblemReporter.ScopedCollector logging = new ProblemReporter.ScopedCollector(blockEntity.problemPath(), IsometricRenders.LOGGER)) {
			blockEntity.loadWithComponents(TagValueInput.create(logging, blockEntity.getLevel().registryAccess(), nbtCopy));
		}
    }
}
