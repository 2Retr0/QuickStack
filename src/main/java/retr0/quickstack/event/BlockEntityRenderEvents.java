package retr0.quickstack.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public final class BlockEntityRenderEvents {
    public static Event<StartRendering> BEGIN = EventFactory.createArrayBacked(StartRendering.class,
            (listeners) -> () -> {
                for (var handler : listeners) {
                    handler.onRenderingStart();
                }
            });

    public static Event<EndRendering> END = EventFactory.createArrayBacked(EndRendering.class,
            (listeners) -> () -> {
                for (var handler : listeners) {
                    handler.onRenderingEnd();
                }
            });

    @FunctionalInterface
    public interface StartRendering {
        void onRenderingStart();
    }

    @FunctionalInterface
    public interface EndRendering {
        void onRenderingEnd();
    }
}
