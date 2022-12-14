package retr0.quickstack.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.util.Util;

import java.util.ArrayDeque;
import java.util.Deque;

public class DelayedCallbackManager {
    public static final DelayedCallbackManager INSTANCE = new DelayedCallbackManager(40);

    private final Deque<Callback> queuedCallbacks = new ArrayDeque<>();

    private long previousInvokeTimeMs = -1;

    public DelayedCallbackManager(int delayMs) {
        ServerTickEvents.START_WORLD_TICK.register(world -> {
            var currentTimeMs = Util.getMeasuringTimeMs();
            if (currentTimeMs - previousInvokeTimeMs >= delayMs && !queuedCallbacks.isEmpty()) {
                queuedCallbacks.poll().invoke();
                previousInvokeTimeMs = currentTimeMs;
            }
        });
    }

    public void scheduleCallback(Callback callback) {
        queuedCallbacks.add(callback);
    }

    @FunctionalInterface
    interface Callback { void invoke(); }
}
