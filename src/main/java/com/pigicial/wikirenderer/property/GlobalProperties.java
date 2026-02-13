package com.pigicial.wikirenderer.property;

import com.pigicial.wikirenderer.render.export.ffmpeg.AnimationHandlingMode;
import com.pigicial.wikirenderer.render.export.ffmpeg.FFmpegDispatcher;

public class GlobalProperties {

    public static final Property<Boolean> SHOW_BACKGROUND_COLOR_IN_EXPORTS = Property.of(false);
    public static int backgroundColor = 0x000000;

    public static final Property<Boolean> UNSAFE = Property.of(false);
    public static final Property<Boolean> SAVE_INTO_ROOT = Property.of(false);
    public static final Property<Boolean> OVERWRITE_LATEST = Property.of(false);

    public static final Property<Boolean> HIDE_NAMETAGS = Property.of(false);
    public static final Property<Boolean> TICK_PARTICLES = Property.of(true);
    public static final Property<Boolean> SPEED_UP_ENCHANTMENT_GLINTS = Property.of(false);
    public static final Property<Boolean> SYNC_ENCHANTMENT_GLINTS_TO_EXPORT = Property.of(false);
    public static final Property<Boolean> SYNC_ROTATION_TO_ANIMATION = Property.of(false);
    public static final Property<Boolean> SET_ANIMATION_FPS_CAP = Property.of(true);

    public static final IntProperty EXPORT_FRAMERATE = IntProperty.of(25, 1, 300);
    public static final IntProperty EXPORT_FRAMES = IntProperty.of(50, 1, 5000);
    public static AnimationHandlingMode animationHandlingMode = AnimationHandlingMode.DISK_INSTANT_SAVE;
    public static FFmpegDispatcher.Format animationFormat = FFmpegDispatcher.Format.GIF;
}
