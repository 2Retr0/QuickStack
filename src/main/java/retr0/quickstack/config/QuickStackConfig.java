package retr0.quickstack.config;

import retr0.carrotconfig.config.CarrotConfig;

public class QuickStackConfig extends CarrotConfig {
    @Entry
    public static boolean allowHotbarQuickStack = false;

    @Entry(min = 0)
    public static int containerSearchRadius = 8;

    @Entry
    public static float containerHighlightDuration = 5.0f;
}
