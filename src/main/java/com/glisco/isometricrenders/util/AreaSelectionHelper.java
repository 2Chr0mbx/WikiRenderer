package com.glisco.isometricrenders.util;

import com.glisco.isometricrenders.render.AreaRenderable;
import com.glisco.isometricrenders.screen.RenderScreen;
import com.glisco.isometricrenders.screen.ScreenScheduler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.SimpleVoxelShape;
import net.minecraft.util.shape.SlicedVoxelShape;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

public class AreaSelectionHelper {

    public static BlockPos pos1 = null;
    public static BlockPos pos2 = null;

    public static boolean shouldDraw() {
        return pos1 != null;
    }

    public static void clear() {
        AreaSelectionHelper.pos1 = null;
        AreaSelectionHelper.pos2 = null;
        Translate.actionBar("selection_cleared");
    }

    public static void renderSelectionBox(MatrixStack matrices, CameraRenderState camera) {
        if (!AreaSelectionHelper.shouldDraw()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        BlockPos origin = AreaSelectionHelper.pos1;

        HitResult result = player.raycast(player.getAbilities().creativeMode ? 5.0F : 4.5F, 0, false);
        BlockPos size = AreaSelectionHelper.pos2 != null ? AreaSelectionHelper.pos2 : (result.getType() == HitResult.Type.BLOCK ? ((BlockHitResult) result).getBlockPos() : BlockPos.ofFloored(result.getPos()));
        size = size.subtract(origin);

        origin = origin.add(size.getX() < 0 ? 1 : 0, size.getY() < 0 ? 1 : 0, size.getZ() < 0 ? 1 : 0);
        size = size.add(size.getX() >= 0 ? 1 : -1, size.getY() >= 0 ? 1 : -1, size.getZ() >= 0 ? 1 : -1);

        matrices.push();

        VertexConsumer consumer = client.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.lines());
        matrices.translate(origin.getX() - camera.pos.x, origin.getY() - camera.pos.y, origin.getZ() - camera.pos.z);

        // VertexRendering.drawOutline(matrices, consumer, VoxelShape, 1, 1, 1, 1, 0, 0, 0);
        VertexRendering.drawOutline(matrices, consumer, VoxelShapes.cuboid(new Box(size)), 0, 0, 0, 1, 1);

        matrices.pop();
    }

    public static void select() {
        final MinecraftClient client = MinecraftClient.getInstance();
        final HitResult target = client.crosshairTarget;
        if ((target == null)) return;
        BlockPos targetPos = new BlockPos(target.getType() == HitResult.Type.BLOCK ? ((BlockHitResult) target).getBlockPos() : BlockPos.ofFloored(target.getPos()));

        if (pos1 == null) {
            pos1 = targetPos;
            Translate.actionBar("selection_started");
        } else {
            Translate.actionBar("selection_finished");
            pos2 = targetPos;
        }
    }

    public static boolean tryOpenScreen() {
        if (pos1 == null || pos2 == null) return false;

        ScreenScheduler.schedule(new RenderScreen(
                AreaRenderable.of(pos1, pos2)
        ));
        return true;
    }
}
