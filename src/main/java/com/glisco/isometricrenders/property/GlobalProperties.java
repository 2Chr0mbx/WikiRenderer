package com.glisco.isometricrenders.property;

import com.glisco.isometricrenders.util.FFmpegDispatcher;

public class GlobalProperties {

    //Render Options
    public static int backgroundColor = 0x000000;

    //Export Options
    public static Property<Boolean> unsafe = Property.of(false);
    public static Property<Boolean> saveIntoRoot = Property.of(false);
    public static Property<Boolean> overwriteLatest = Property.of(false);

    // Animation Options
    public static Property<Boolean> speedUpEnchantmentGlints = Property.of(false);
    public static IntProperty exportFramerate = IntProperty.of(30, 1, 300);
    public static IntProperty exportFrames = IntProperty.of(60, 1, 5000);

    public static FFmpegDispatcher.Format animationFormat = FFmpegDispatcher.Format.GIF;
}
