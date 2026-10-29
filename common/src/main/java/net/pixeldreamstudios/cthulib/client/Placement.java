package net.pixeldreamstudios.cthulib.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.pixeldreamstudios.cthulib.client.base.DraggableTitleScreenWidget;
import net.pixeldreamstudios.cthulib.config.CthuLibConfig;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public final class Placement {

    public static final String ELEMENT_PREFIX = "element:";
    public static final String POINT_SEPARATOR = "@";
    public static final String ANCHOR_HINT = "none, top-left ... bottom-right, or element:menu.singleplayer@top-right";

    public enum Anchor {
        TOP_LEFT("top-left", 0.0f, 0.0f),
        MID_LEFT("mid-left", 0.0f, 0.5f),
        BOTTOM_LEFT("bottom-left", 0.0f, 1.0f),
        TOP_CENTERED("top-centered", 0.5f, 0.0f),
        MID_CENTERED("mid-centered", 0.5f, 0.5f),
        BOTTOM_CENTERED("bottom-centered", 0.5f, 1.0f),
        TOP_RIGHT("top-right", 1.0f, 0.0f),
        MID_RIGHT("mid-right", 1.0f, 0.5f),
        BOTTOM_RIGHT("bottom-right", 1.0f, 1.0f);

        private final String id;
        private final float xFraction;
        private final float yFraction;

        Anchor(String id, float xFraction, float yFraction) {
            this.id = id;
            this.xFraction = xFraction;
            this.yFraction = yFraction;
        }

        public String id() {
            return id;
        }

        public int x(int left, int width) {
            return left + Math.round(width * xFraction);
        }

        public int y(int top, int height) {
            return top + Math.round(height * yFraction);
        }

        public static Optional<Anchor> of(String id) {
            String key = id == null ? "" : id.trim().toLowerCase(Locale.ROOT).replace('_', '-');
            return Arrays.stream(values()).filter(anchor -> anchor.id.equals(key)).findFirst();
        }
    }

    public record Origin(int x, int y) {
    }

    public interface FloatGetter {
        float get(CthuLibConfig config);
    }

    public interface FloatSetter {
        void set(CthuLibConfig config, float value);
    }

    public record IntField(ToIntFunction<CthuLibConfig> get, ObjIntConsumer<CthuLibConfig> set) {
    }

    public record FloatField(FloatGetter get, FloatSetter set) {
    }

    public record BoolField(Predicate<CthuLibConfig> get, BiConsumer<CthuLibConfig, Boolean> set) {
    }

    public record TextField(Function<CthuLibConfig, String> get, BiConsumer<CthuLibConfig, String> set) {
    }

    public record Spec(int x, int y, int width, int height, float scale, boolean scaleWithScreen, float minScale, float maxScale,
                       boolean useAnchor, boolean centerX, boolean centerY, boolean leftX, boolean rightX, String anchor) {

        public int scaledWidth() {
            return (int) (width * scale);
        }

        public int scaledHeight() {
            return (int) (height * scale);
        }

        public boolean anchored() {
            return anchor != null && !anchor.isBlank();
        }

        public boolean legacy() {
            return !anchored() && !useAnchor && !centerX && !centerY && !leftX && !rightX && !scaleWithScreen;
        }
    }

    public record Access(IntField x, IntField y, IntField width, IntField height, FloatField scale, BoolField scaleWithScreen,
                         FloatField minScale, FloatField maxScale, BoolField useAnchor, BoolField centerX, BoolField centerY,
                         BoolField leftX, BoolField rightX, TextField anchor) {

        public Spec spec(CthuLibConfig config) {
            return new Spec(x.get().applyAsInt(config), y.get().applyAsInt(config), width.get().applyAsInt(config), height.get().applyAsInt(config),
                    scale.get().get(config), scaleWithScreen.get().test(config), minScale.get().get(config), maxScale.get().get(config),
                    useAnchor.get().test(config), centerX.get().test(config), centerY.get().test(config), leftX.get().test(config),
                    rightX.get().test(config), anchor.get().apply(config));
        }
    }

    private Placement() {
    }

    public static Optional<Origin> origin(String anchor, int screenWidth, int screenHeight, String self) {
        if (anchor == null || anchor.isBlank()) {
            return Optional.empty();
        }
        String text = anchor.trim();
        if (!text.toLowerCase(Locale.ROOT).startsWith(ELEMENT_PREFIX)) {
            return Anchor.of(text).map(point -> new Origin(point.x(0, screenWidth), point.y(0, screenHeight)));
        }
        String target = text.substring(ELEMENT_PREFIX.length());
        Anchor point = Anchor.TOP_LEFT;
        int split = target.lastIndexOf(POINT_SEPARATOR);
        if (split > 0) {
            Optional<Anchor> parsed = Anchor.of(target.substring(split + 1));
            if (parsed.isPresent()) {
                point = parsed.get();
                target = target.substring(0, split);
            }
        }
        Anchor corner = point;
        return find(target.trim(), self).map(widget -> new Origin(corner.x(widget.getX(), widget.getWidth()), corner.y(widget.getY(), widget.getHeight())));
    }

    public static Optional<AbstractWidget> find(String target, String self) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null || target.isEmpty()) {
            return Optional.empty();
        }
        for (GuiEventListener child : screen.children()) {
            if (!(child instanceof AbstractWidget widget) || !widget.visible) {
                continue;
            }
            if (widget instanceof DraggableTitleScreenWidget own) {
                String id = own.getWidgetIdentifierFancyMenu();
                if (id != null && id.equals(target) && !id.equals(self)) {
                    return Optional.of(widget);
                }
                continue;
            }
            if (widget.getMessage().getContents() instanceof TranslatableContents translatable && translatable.getKey().equals(target)) {
                return Optional.of(widget);
            }
            if (widget.getMessage().getString().equalsIgnoreCase(target)) {
                return Optional.of(widget);
            }
        }
        return Optional.empty();
    }
}
