package net.pixeldreamstudios.cthulib.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

public final class AnimationPlayer {
    public record Region(int x, int y, int width, int height) {
        Region union(Region other) {
            if (other == null) {
                return this;
            }
            int minX = Math.min(x, other.x);
            int minY = Math.min(y, other.y);
            int maxX = Math.max(x + width, other.x + other.width);
            int maxY = Math.max(y + height, other.y + other.height);
            return new Region(minX, minY, maxX - minX, maxY - minY);
        }
    }

    private static final long PAUSE_GAP_MS = 1000;
    private static final int CHANNEL_MAX = 0xFF;

    private final AnimatedImage image;
    private final Executor executor;
    private final int[] canvas;
    private int[] savedRegion;
    private int shown = -1;
    private int playsDone = 0;
    private boolean finished = false;
    private long deadline = 0;
    private long lastUpdate = -1;
    private int nextIndex = 0;
    private CompletableFuture<int[]> next;

    public AnimationPlayer(AnimatedImage image, int[] firstFrame, Executor executor) {
        this.image = image;
        this.executor = executor;
        this.canvas = new int[image.width() * image.height()];
        this.next = CompletableFuture.completedFuture(firstFrame);
    }

    public int width() {
        return image.width();
    }

    public int height() {
        return image.height();
    }

    public int[] canvas() {
        return canvas;
    }

    public Optional<Region> update(long now) {
        if (lastUpdate >= 0 && now - lastUpdate > PAUSE_GAP_MS) {
            deadline += now - lastUpdate;
        }
        lastUpdate = now;
        if (finished || (shown >= 0 && now < deadline) || !next.isDone()) {
            return Optional.empty();
        }

        int[] pixels;
        try {
            pixels = next.join();
        } catch (CompletionException e) {
            CthuLibLogger.error("Animated image frame failed: " + e.getMessage());
            finished = true;
            return Optional.empty();
        }

        Region dirty = shown >= 0 ? dispose(image.frames().get(shown)) : null;
        if (nextIndex == 0) {
            Arrays.fill(canvas, 0);
            dirty = new Region(0, 0, image.width(), image.height());
        }

        AnimatedImage.Frame frame = image.frames().get(nextIndex);
        Region area = clip(frame);
        if (frame.disposal() == AnimatedImage.Disposal.RESTORE_PREVIOUS && area != null) {
            savedRegion = copyRegion(area);
        }
        if (area != null) {
            draw(frame, area, pixels);
            dirty = area.union(dirty);
        }

        long start = shown < 0 || now - deadline > frame.delayMs() ? now : deadline;
        deadline = start + frame.delayMs();
        shown = nextIndex;
        scheduleFollowing();
        return Optional.ofNullable(dirty);
    }

    private void scheduleFollowing() {
        int following = shown + 1;
        if (following >= image.frames().size()) {
            playsDone++;
            if (image.plays() > 0 && playsDone >= image.plays()) {
                finished = true;
                return;
            }
            following = 0;
        }
        int index = following;
        nextIndex = index;
        next = CompletableFuture.supplyAsync(() -> {
            try {
                return image.decodeFrame(index);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, executor);
    }

    private Region dispose(AnimatedImage.Frame frame) {
        Region area = clip(frame);
        if (area == null) {
            return null;
        }
        switch (frame.disposal()) {
            case CLEAR -> {
                for (int y = area.y(); y < area.y() + area.height(); y++) {
                    int row = y * image.width();
                    Arrays.fill(canvas, row + area.x(), row + area.x() + area.width(), 0);
                }
                return area;
            }
            case RESTORE_PREVIOUS -> {
                if (savedRegion == null) {
                    return null;
                }
                for (int y = 0; y < area.height(); y++) {
                    System.arraycopy(savedRegion, y * area.width(), canvas, (area.y() + y) * image.width() + area.x(), area.width());
                }
                savedRegion = null;
                return area;
            }
            default -> {
                return null;
            }
        }
    }

    private int[] copyRegion(Region area) {
        int[] copy = new int[area.width() * area.height()];
        for (int y = 0; y < area.height(); y++) {
            System.arraycopy(canvas, (area.y() + y) * image.width() + area.x(), copy, y * area.width(), area.width());
        }
        return copy;
    }

    private void draw(AnimatedImage.Frame frame, Region area, int[] pixels) {
        for (int y = area.y(); y < area.y() + area.height(); y++) {
            int sourceRow = (y - frame.y()) * frame.width() - frame.x();
            int targetRow = y * image.width();
            for (int x = area.x(); x < area.x() + area.width(); x++) {
                int source = pixels[sourceRow + x];
                canvas[targetRow + x] = frame.blend() ? blend(canvas[targetRow + x], source) : source;
            }
        }
    }

    private Region clip(AnimatedImage.Frame frame) {
        int minX = Math.max(0, frame.x());
        int minY = Math.max(0, frame.y());
        int maxX = Math.min(image.width(), frame.x() + frame.width());
        int maxY = Math.min(image.height(), frame.y() + frame.height());
        if (maxX <= minX || maxY <= minY) {
            return null;
        }
        return new Region(minX, minY, maxX - minX, maxY - minY);
    }

    private static int blend(int destination, int source) {
        int sourceAlpha = source >>> 24;
        if (sourceAlpha == CHANNEL_MAX) {
            return source;
        }
        if (sourceAlpha == 0) {
            return destination;
        }
        int destinationAlpha = destination >>> 24;
        int destinationWeight = destinationAlpha * (CHANNEL_MAX - sourceAlpha) / CHANNEL_MAX;
        int outAlpha = sourceAlpha + destinationWeight;
        int red = (channel(source, 16) * sourceAlpha + channel(destination, 16) * destinationWeight) / outAlpha;
        int green = (channel(source, 8) * sourceAlpha + channel(destination, 8) * destinationWeight) / outAlpha;
        int blue = (channel(source, 0) * sourceAlpha + channel(destination, 0) * destinationWeight) / outAlpha;
        return outAlpha << 24 | red << 16 | green << 8 | blue;
    }

    private static int channel(int argb, int shift) {
        return argb >>> shift & CHANNEL_MAX;
    }
}
