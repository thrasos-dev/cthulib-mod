package net.pixeldreamstudios.cthulib.util;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public final class AnimatedImage {
    public enum Format {
        GIF("gif"),
        WEBP("webp");

        public final String extension;

        Format(String extension) {
            this.extension = extension;
        }
    }

    public enum Disposal {
        KEEP,
        CLEAR,
        RESTORE_PREVIOUS
    }

    public record Frame(int x, int y, int width, int height, int delayMs, boolean blend, Disposal disposal) {
    }

    private interface FrameDecoder {
        BufferedImage decode(int index) throws IOException;
    }

    private static final int SHORT_DELAY_LIMIT_MS = 10;
    private static final int DEFAULT_DELAY_MS = 100;
    private static final int GIF_DELAY_UNIT_MS = 10;
    private static final int MAX_CANVAS_SIDE = 4096;
    private static final int MAX_FRAMES = 2000;
    private static final int INFINITE_PLAYS = 0;

    private static final byte[] GIF_MAGIC = "GIF8".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] RIFF_MAGIC = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP_MAGIC = "WEBP".getBytes(StandardCharsets.US_ASCII);
    private static final int WEBP_MAGIC_OFFSET = 8;
    private static final int RIFF_HEADER_SIZE = 12;
    private static final int CHUNK_HEADER_SIZE = 8;
    private static final int VP8X_BODY_SIZE = 10;
    private static final int VP8X_ANIMATION_FLAG = 0x02;
    private static final int VP8X_ALPHA_FLAG = 0x10;
    private static final int ANMF_HEADER_SIZE = 16;
    private static final int ANMF_NO_BLEND_FLAG = 0x02;
    private static final int ANMF_DISPOSE_FLAG = 0x01;
    private static final String CHUNK_VP8X = "VP8X";
    private static final String CHUNK_ANIM = "ANIM";
    private static final String CHUNK_ANMF = "ANMF";
    private static final String CHUNK_ALPH = "ALPH";
    private static final String CHUNK_VP8 = "VP8 ";
    private static final String CHUNK_VP8L = "VP8L";

    private static final String GIF_FORMAT_NAME = "gif";
    private static final String GIF_STREAM_METADATA = "javax_imageio_gif_stream_1.0";
    private static final String GIF_IMAGE_METADATA = "javax_imageio_gif_image_1.0";
    private static final String NETSCAPE_ID = "NETSCAPE";
    private static final String NETSCAPE_AUTH = "2.0";

    private final Format format;
    private final int width;
    private final int height;
    private final int plays;
    private final List<Frame> frames;
    private final FrameDecoder decoder;

    private AnimatedImage(Format format, int width, int height, int plays, List<Frame> frames, FrameDecoder decoder) {
        this.format = format;
        this.width = width;
        this.height = height;
        this.plays = plays;
        this.frames = frames;
        this.decoder = decoder;
    }

    public Format format() {
        return format;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int plays() {
        return plays;
    }

    public List<Frame> frames() {
        return frames;
    }

    public static Optional<Format> rawCacheFormat(byte[] data) {
        if (startsWith(data, 0, GIF_MAGIC)) {
            return Optional.of(Format.GIF);
        }
        if (isWebp(data) && isAnimatedWebp(data)) {
            return Optional.of(Format.WEBP);
        }
        return Optional.empty();
    }

    public static Optional<AnimatedImage> read(byte[] data) {
        try {
            if (startsWith(data, 0, GIF_MAGIC)) {
                return readGif(data);
            }
            if (isWebp(data)) {
                return readWebp(data);
            }
        } catch (IOException | RuntimeException e) {
            CthuLibLogger.error("Could not read animated image: " + e.getMessage());
        }
        return Optional.empty();
    }

    public synchronized int[] decodeFrame(int index) throws IOException {
        Frame frame = frames.get(index);
        BufferedImage image = decoder.decode(index);
        if (image == null) {
            throw new IOException("No decoder for frame " + index);
        }
        int[] pixels = new int[frame.width() * frame.height()];
        image.getRGB(0, 0, Math.min(frame.width(), image.getWidth()), Math.min(frame.height(), image.getHeight()),
                pixels, 0, frame.width());
        return pixels;
    }

    private static Optional<AnimatedImage> readGif(byte[] data) throws IOException {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(GIF_FORMAT_NAME);
        if (!readers.hasNext()) {
            return Optional.empty();
        }
        ImageReader reader = readers.next();
        reader.setInput(new MemoryCacheImageInputStream(new ByteArrayInputStream(data)), false, false);
        int count = reader.getNumImages(true);
        if (count < 2 || count > MAX_FRAMES) {
            reader.dispose();
            return Optional.empty();
        }

        Node screen = child(reader.getStreamMetadata().getAsTree(GIF_STREAM_METADATA), "LogicalScreenDescriptor");
        int width = screen != null ? intAttribute(screen, "logicalScreenWidth", 0) : 0;
        int height = screen != null ? intAttribute(screen, "logicalScreenHeight", 0) : 0;
        int plays = 1;
        List<Frame> frames = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            Node root = reader.getImageMetadata(i).getAsTree(GIF_IMAGE_METADATA);
            Node descriptor = child(root, "ImageDescriptor");
            Node control = child(root, "GraphicControlExtension");
            if (descriptor == null) {
                throw new IOException("GIF frame " + i + " has no image descriptor");
            }
            int x = intAttribute(descriptor, "imageLeftPosition", 0);
            int y = intAttribute(descriptor, "imageTopPosition", 0);
            int frameWidth = intAttribute(descriptor, "imageWidth", 0);
            int frameHeight = intAttribute(descriptor, "imageHeight", 0);
            int delay = control != null ? intAttribute(control, "delayTime", 0) * GIF_DELAY_UNIT_MS : 0;
            String disposal = control != null ? attribute(control, "disposalMethod") : null;
            frames.add(new Frame(x, y, frameWidth, frameHeight, normalizeDelay(delay), true, gifDisposal(disposal)));
            width = Math.max(width, x + frameWidth);
            height = Math.max(height, y + frameHeight);
            if (i == 0) {
                plays = gifPlays(root);
            }
        }

        if (!validCanvas(width, height)) {
            reader.dispose();
            return Optional.empty();
        }
        return Optional.of(new AnimatedImage(Format.GIF, width, height, plays, List.copyOf(frames), index -> reader.read(index)));
    }

    private static Disposal gifDisposal(String method) {
        if ("restoreToBackgroundColor".equals(method)) {
            return Disposal.CLEAR;
        }
        if ("restoreToPrevious".equals(method)) {
            return Disposal.RESTORE_PREVIOUS;
        }
        return Disposal.KEEP;
    }

    private static int gifPlays(Node imageRoot) {
        Node extensions = child(imageRoot, "ApplicationExtensions");
        if (extensions == null) {
            return 1;
        }
        for (Node node = extensions.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (NETSCAPE_ID.equals(attribute(node, "applicationID")) && NETSCAPE_AUTH.equals(attribute(node, "authenticationCode"))
                    && node instanceof IIOMetadataNode metadataNode && metadataNode.getUserObject() instanceof byte[] bytes && bytes.length >= 3) {
                int repeats = (bytes[1] & 0xFF) | (bytes[2] & 0xFF) << 8;
                return repeats == 0 ? INFINITE_PLAYS : repeats + 1;
            }
        }
        return 1;
    }

    private static boolean isWebp(byte[] data) {
        return startsWith(data, 0, RIFF_MAGIC) && startsWith(data, WEBP_MAGIC_OFFSET, WEBP_MAGIC);
    }

    private static boolean isAnimatedWebp(byte[] data) {
        int end = riffEnd(data);
        int pos = RIFF_HEADER_SIZE;
        while (pos + CHUNK_HEADER_SIZE <= end) {
            int size = u32(data, pos + 4);
            int body = pos + CHUNK_HEADER_SIZE;
            if (size < 0 || body + size > end) {
                return false;
            }
            if (CHUNK_VP8X.equals(tag(data, pos))) {
                return size > 0 && (data[body] & VP8X_ANIMATION_FLAG) != 0;
            }
            pos = body + size + (size & 1);
        }
        return false;
    }

    private static Optional<AnimatedImage> readWebp(byte[] data) throws IOException {
        int end = riffEnd(data);
        int pos = RIFF_HEADER_SIZE;
        boolean animated = false;
        int width = 0;
        int height = 0;
        int plays = INFINITE_PLAYS;
        List<Frame> frames = new ArrayList<>();
        List<byte[]> stills = new ArrayList<>();

        while (pos + CHUNK_HEADER_SIZE <= end) {
            String tag = tag(data, pos);
            int size = u32(data, pos + 4);
            int body = pos + CHUNK_HEADER_SIZE;
            if (size < 0 || body + size > end) {
                break;
            }
            switch (tag) {
                case CHUNK_VP8X -> {
                    animated = (data[body] & VP8X_ANIMATION_FLAG) != 0;
                    width = u24(data, body + 4) + 1;
                    height = u24(data, body + 7) + 1;
                }
                case CHUNK_ANIM -> plays = u16(data, body + 4);
                case CHUNK_ANMF -> {
                    if (size < ANMF_HEADER_SIZE || frames.size() >= MAX_FRAMES) {
                        throw new IOException("Bad WebP animation frame");
                    }
                    int frameWidth = u24(data, body + 6) + 1;
                    int frameHeight = u24(data, body + 9) + 1;
                    int flags = data[body + 15];
                    frames.add(new Frame(u24(data, body) * 2, u24(data, body + 3) * 2, frameWidth, frameHeight,
                            normalizeDelay(u24(data, body + 12)), (flags & ANMF_NO_BLEND_FLAG) == 0,
                            (flags & ANMF_DISPOSE_FLAG) != 0 ? Disposal.CLEAR : Disposal.KEEP));
                    stills.add(stillWebp(data, body + ANMF_HEADER_SIZE, body + size, frameWidth, frameHeight));
                }
                default -> {
                }
            }
            pos = body + size + (size & 1);
        }

        if (!animated || frames.size() < 2 || !validCanvas(width, height)) {
            return Optional.empty();
        }
        List<byte[]> frameFiles = List.copyOf(stills);
        return Optional.of(new AnimatedImage(Format.WEBP, width, height, plays, List.copyOf(frames),
                index -> ImageIO.read(new ByteArrayInputStream(frameFiles.get(index)))));
    }

    private static byte[] stillWebp(byte[] data, int from, int to, int width, int height) throws IOException {
        int alphaStart = -1;
        int alphaEnd = -1;
        int imageStart = -1;
        int imageEnd = -1;
        boolean lossless = false;
        int pos = from;
        while (pos + CHUNK_HEADER_SIZE <= to) {
            String tag = tag(data, pos);
            int size = u32(data, pos + 4);
            int chunkEnd = Math.min(to, pos + CHUNK_HEADER_SIZE + size + (size & 1));
            if (size < 0 || pos + CHUNK_HEADER_SIZE + size > to) {
                break;
            }
            if (CHUNK_ALPH.equals(tag)) {
                alphaStart = pos;
                alphaEnd = chunkEnd;
            } else if (CHUNK_VP8.equals(tag) || CHUNK_VP8L.equals(tag)) {
                imageStart = pos;
                imageEnd = chunkEnd;
                lossless = CHUNK_VP8L.equals(tag);
                break;
            }
            pos = chunkEnd;
        }
        if (imageStart < 0) {
            throw new IOException("WebP frame has no image data");
        }

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.writeBytes(WEBP_MAGIC);
        if (!lossless && alphaStart >= 0) {
            body.writeBytes(CHUNK_VP8X.getBytes(StandardCharsets.US_ASCII));
            writeU32(body, VP8X_BODY_SIZE);
            body.write(VP8X_ALPHA_FLAG);
            body.write(0);
            body.write(0);
            body.write(0);
            writeU24(body, width - 1);
            writeU24(body, height - 1);
            body.write(data, alphaStart, alphaEnd - alphaStart);
        }
        body.write(data, imageStart, imageEnd - imageStart);

        ByteArrayOutputStream file = new ByteArrayOutputStream(body.size() + CHUNK_HEADER_SIZE);
        file.writeBytes(RIFF_MAGIC);
        writeU32(file, body.size());
        body.writeTo(file);
        return file.toByteArray();
    }

    private static int normalizeDelay(int delayMs) {
        return delayMs <= SHORT_DELAY_LIMIT_MS ? DEFAULT_DELAY_MS : delayMs;
    }

    private static boolean validCanvas(int width, int height) {
        return width > 0 && height > 0 && width <= MAX_CANVAS_SIDE && height <= MAX_CANVAS_SIDE;
    }

    private static int riffEnd(byte[] data) {
        return (int) Math.min(data.length, CHUNK_HEADER_SIZE + (u32(data, 4) & 0xFFFFFFFFL));
    }

    private static boolean startsWith(byte[] data, int offset, byte[] prefix) {
        if (data.length < offset + prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[offset + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static String tag(byte[] data, int pos) {
        return new String(data, pos, 4, StandardCharsets.US_ASCII);
    }

    private static int u16(byte[] data, int pos) {
        return (data[pos] & 0xFF) | (data[pos + 1] & 0xFF) << 8;
    }

    private static int u24(byte[] data, int pos) {
        return u16(data, pos) | (data[pos + 2] & 0xFF) << 16;
    }

    private static int u32(byte[] data, int pos) {
        return u24(data, pos) | (data[pos + 3] & 0xFF) << 24;
    }

    private static void writeU24(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write(value >>> 8 & 0xFF);
        out.write(value >>> 16 & 0xFF);
    }

    private static void writeU32(ByteArrayOutputStream out, int value) {
        writeU24(out, value);
        out.write(value >>> 24 & 0xFF);
    }

    private static Node child(Node parent, String name) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (name.equals(node.getNodeName())) {
                return node;
            }
        }
        return null;
    }

    private static String attribute(Node node, String name) {
        NamedNodeMap attributes = node.getAttributes();
        Node value = attributes != null ? attributes.getNamedItem(name) : null;
        return value != null ? value.getNodeValue() : null;
    }

    private static int intAttribute(Node node, String name, int fallback) {
        String value = attribute(node, name);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
