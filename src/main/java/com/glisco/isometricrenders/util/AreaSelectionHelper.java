package com.glisco.isometricrenders.util;

import com.glisco.isometricrenders.render.area.AreaRenderable;
import com.glisco.isometricrenders.screen.RenderScreen;
import com.glisco.isometricrenders.screen.ScreenSchedulerAndSaver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

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

    public static void renderSelectionBox() {
        if (!AreaSelectionHelper.shouldDraw()) return;

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;

        BlockPos pos1 = AreaSelectionHelper.pos1;

        HitResult result = player.pick(player.getAbilities().instabuild ? 5.0F : 4.5F, 0, false);
        BlockPos pos2 = AreaSelectionHelper.pos2 != null ? AreaSelectionHelper.pos2 : (result.getType() == HitResult.Type.BLOCK ? ((BlockHitResult) result).getBlockPos() : BlockPos.containing(result.getLocation()));

        // recalibrate to make math easier
        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        pos1 = new BlockPos(minX, minY, minZ);
        pos2 = new BlockPos(maxX + 1, maxY + 1, maxZ + 1);

        Gizmos.cuboid(new AABB(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ()), GizmoStyle.stroke(ARGB.colorFromFloat(1, 1, 1, 1)));
    }

    public static void select() {
        final Minecraft client = Minecraft.getInstance();
        final HitResult target = client.hitResult;
        if ((target == null)) return;
        BlockPos targetPos = new BlockPos(target.getType() == HitResult.Type.BLOCK ? ((BlockHitResult) target).getBlockPos() : BlockPos.containing(target.getLocation()));

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

        ScreenSchedulerAndSaver.schedule(new RenderScreen(
                AreaRenderable.of(pos1, pos2)
        ));
        return true;
    }
}
