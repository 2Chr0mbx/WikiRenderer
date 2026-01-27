package com.glisco.isometricrenders.util;

import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.property.PropertyBundle;
import com.glisco.isometricrenders.render.Renderable;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.joml.Quaternionf;

public class CameraOrientationUtil {

    public static CameraRenderState createRenderState(Renderable<? extends DefaultPropertyBundle> renderable) {
        return createRenderState(renderable.getProperties());
    }

    public static CameraRenderState createRenderState(DefaultPropertyBundle properties) {
        CameraRenderState state = new CameraRenderState();
        state.orientation = orientQuaternion(properties);
        return state;
    }

    public static Quaternionf orientQuaternion(DefaultPropertyBundle properties) {
        return new Quaternionf().rotationYXZ(
                (float) Math.PI - (float) Math.toRadians(properties.getUsedRotation()),
                (float) Math.PI + (float) Math.toRadians(properties.getUsedSlant()),
                (float) Math.PI);
    }
}
