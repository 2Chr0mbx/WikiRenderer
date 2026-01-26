package com.glisco.isometricrenders.util;

import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.render.Renderable;
import net.minecraft.client.renderer.state.CameraRenderState;

public class CameraOrientationUtil {

    public static CameraRenderState createRenderState(Renderable<? extends DefaultPropertyBundle> renderable) {
        CameraRenderState state = new CameraRenderState();
        state.orientation.rotationYXZ(
                (float) Math.PI - (float) Math.toRadians(renderable.getProperties().getUsedRotation()),
                (float) Math.PI + (float) Math.toRadians(renderable.getProperties().getUsedSlant()),
                (float) Math.PI);
        return state;
    }
}
