package com.pigicial.wikirenderer.util;

import com.pigicial.wikirenderer.render.area.AreaRenderable;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.screen.ScreenSchedulerAndSaver;
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
        if (player == null) return;

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

        Gizmos.cuboid(new AABB(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ()), GizmoStyle.stroke(ARGB.colorFromFloat(1, 1, 1, 1f), 5), true);
    }

    public static void select() {
        HitResult target = Minecraft.getInstance().hitResult;
        if (target == null) {
            return;
        }

        BlockPos targetPos = new BlockPos(target.getType() == HitResult.Type.BLOCK ? ((BlockHitResult) target).getBlockPos() : BlockPos.containing(target.getLocation()));

        if (pos1 == null) {
            pos1 = targetPos;
            Translate.actionBar("selection_started");
        } else {
            Translate.actionBar("selection_finished");
            pos2 = targetPos;
        }
    }

    public static void expand() {
        HitResult target = Minecraft.getInstance().hitResult;
        if (target == null) {
            return;
        }

        BlockPos targetPos = new BlockPos(target.getType() == HitResult.Type.BLOCK ? ((BlockHitResult) target).getBlockPos() : BlockPos.containing(target.getLocation()));

        if (pos1 == null) {
            pos1 = targetPos;
            Translate.actionBar("selection_started");
        } else {
            if (pos2 == null) {
                Translate.actionBar("selection_finished");
                pos2 = targetPos;
            } else {
                Translate.actionBar("selection_expanded");

                int minX = Math.min(Math.min(pos1.getX(), pos2.getX()), targetPos.getX());
                int maxX = Math.max(Math.max(pos1.getX(), pos2.getX()), targetPos.getX());
                int minY = Math.min(Math.min(pos1.getY(), pos2.getY()), targetPos.getY());
                int maxY = Math.max(Math.max(pos1.getY(), pos2.getY()), targetPos.getY());
                int minZ = Math.min(Math.min(pos1.getZ(), pos2.getZ()), targetPos.getZ());
                int maxZ = Math.max(Math.max(pos1.getZ(), pos2.getZ()), targetPos.getZ());
                pos1 = new BlockPos(minX, minY, minZ);
                pos2 = new BlockPos(maxX, maxY, maxZ);
            }
        }
    }

    public static boolean tryOpenScreen() {
        if (pos1 == null || pos2 == null) return false;

        ScreenSchedulerAndSaver.schedule(new RenderScreen(AreaRenderable.of(pos1, pos2)));
        return true;
    }
}
