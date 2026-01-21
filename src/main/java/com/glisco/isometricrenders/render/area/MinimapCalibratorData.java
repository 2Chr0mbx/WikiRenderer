package com.glisco.isometricrenders.render.area;

import com.glisco.isometricrenders.property.GlobalProperties;
import com.glisco.isometricrenders.util.ImageCropper;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;

/**
 * For usage on <a href="https://hypixel-skyblock.fandom.com/wiki/Module:Minimap/Datasheet">the Hypixel SkyBlock Fandom wiki's Module:Minimap/Datasheet Minimap Calibrator tool</a>
 */
public record MinimapCalibratorData(
        int topLeftImagePixelX,
        int topLeftImagePixelY,
        double topLeftMapCoordX,
        double topLeftMapCoordY,
        int bottomRightImagePixelX,
        int bottomRightImagePixelY,
        double bottomRightMapCoordX,
        double bottomRightMapCoordY,
        int imageWidth,
        int imageHeight
) {

    // this is a little but not entirely black magic to me
    public static MinimapCalibratorData getCalibrationData(AreaRenderable renderable, @Nullable ImageCropper.CropData cropData, NativeImage image) {
        Map<ViewportCorner, Vec3> corners = MinimapCalibratorData.getViewportCorners(renderable);
        Vec3 originTopLeft = corners.get(ViewportCorner.TOP_LEFT);

        double resolution = GlobalProperties.exportResolution;
        Vec3 right = corners.get(ViewportCorner.TOP_RIGHT).subtract(originTopLeft).scale(1.0 / resolution);
        Vec3 down = corners.get(ViewportCorner.BOTTOM_LEFT).subtract(originTopLeft).scale(1.0 / resolution);

        int cropX = (cropData != null) ? cropData.minX() : 0;
        int cropY = (cropData != null) ? cropData.minY() : 0;

        Vec3 worldTopLeft = originTopLeft
                .add(right.scale(cropX))
                .add(down.scale(cropY));

        Vec3 worldBottomRight = worldTopLeft
                .add(right.scale(image.getWidth() - 1))
                .add(down.scale(image.getHeight() - 1));

        Vec2 mapTopLeft = projectToMapPlane(renderable, worldTopLeft);
        Vec2 mapBottomRight = projectToMapPlane(renderable, worldBottomRight);

        MinimapCalibratorData minimapCalibratorData = new MinimapCalibratorData(
                0, 0, mapTopLeft.x, mapTopLeft.y,
                image.getWidth() - 1, image.getHeight() - 1, mapBottomRight.x, mapBottomRight.y,
                image.getWidth(), image.getHeight()
        );
        System.out.println("data = " + minimapCalibratorData);
        return minimapCalibratorData;
    }

    private static Map<ViewportCorner, Vec3> getViewportCorners(AreaRenderable renderable) {
        Matrix4f projection = new Matrix4f().setOrtho(-1, 1, -1, 1, -1000, 3000);

        Matrix4fStack modelView = new Matrix4fStack(2);
        renderable.properties().applyToViewMatrix(renderable, modelView);
        modelView.translate(-renderable.xSize / 2f, -renderable.ySize / 2f, -renderable.zSize / 2f);

        Matrix4f combined = new Matrix4f(projection).mul(modelView);
        combined.invert();

        Map<ViewportCorner, Vec3> corners = new HashMap<>();
        for (ViewportCorner corner : ViewportCorner.values()) {
            Vec3 unprojectedCoords = MinimapCalibratorData.unproject(renderable, combined, corner.getNDCX(), corner.getNDCY());
            corners.put(corner, unprojectedCoords);
        }


        return corners;
    }

    private static Vec2 projectToMapPlane(AreaRenderable renderable, Vec3 worldPosition) {
        AreaRenderable.AreaPropertyBundle props = renderable.properties();

        return switch (props.sideViewSlant) {
            case BELOW, ABOVE -> new Vec2((float) worldPosition.x, (float) worldPosition.z);
            case SIDE -> switch (props.sideViewRotation) {
                case NORTH, SOUTH -> new Vec2((float) worldPosition.x, (float) worldPosition.y);
                case EAST, WEST -> new Vec2((float) worldPosition.z, (float) worldPosition.y);
            };
        };
    }

    private static Vec3 unproject(AreaRenderable renderable, Matrix4f invMatrix, float ndcX, float ndcY) {
        Vector4f pos = new Vector4f(ndcX, ndcY, 0, 1);
        pos.mul(invMatrix);

        double pixelsPerBlock = GlobalProperties.sideViewPixelsPerBlockResolution;
        float halfPixelWorldOffset = 0;
        if (pixelsPerBlock == 4 && GlobalProperties.halfPixelOffsetFor4x4.get()) {
            halfPixelWorldOffset = 0.5f / (float) pixelsPerBlock;
        }

        return new Vec3(pos.x - halfPixelWorldOffset, pos.y, pos.z - halfPixelWorldOffset).add(renderable.mesh.startPos().getX(), renderable.mesh.startPos().getY(), renderable.mesh.startPos().getZ());
    }
}
