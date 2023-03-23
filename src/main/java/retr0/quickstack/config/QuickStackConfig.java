package retr0.quickstack.config;

import retr0.carrotconfig.config.CarrotConfig;

public class QuickStackConfig extends CarrotConfig {
    @Entry
    public static boolean roundedIconBackground = false;

    @Entry(min = 0)
    public static float maxInhabitedTime = 120.0f;

    @Entry(min = 0)
    public static float toastCooldownTime = 30.0f;
}
