package interpreter;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public final class AsciiAnimationService {
    private static final long DEFAULT_FRAME_DURATION_MS = 1000L / 12L;

    private AsciiAnimationService() {
    }

    public static MediaType detectMediaType(Path sourcePath) {
        String name = sourcePath.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".gif")) {
            return MediaType.GIF;
        }
        if (name.endsWith(".mp4") || name.endsWith(".webm") || name.endsWith(".avi") || name.endsWith(".mov") || name.endsWith(".mkv")) {
            return MediaType.VIDEO;
        }
        return MediaType.IMAGE;
    }

    public static boolean isAnimated(MediaType mediaType) {
        return mediaType == MediaType.GIF || mediaType == MediaType.VIDEO;
    }

    public static AsciiAnimation loadAnimation(AsciiRenderPlan plan) throws IOException {
        MediaType mediaType = detectMediaType(plan.getSourcePath());
        return switch (mediaType) {
            case GIF -> loadGif(plan);
            case VIDEO -> loadVideo(plan);
            case IMAGE -> loadSingleImage(plan);
        };
    }

    public static void exportAnimationFrames(AsciiAnimation animation, Path outputDirectory) throws IOException {
        ensureDirectory(outputDirectory);
        for (AsciiFrame frame : animation.getFrames()) {
            Path framePath = outputDirectory.resolve(String.format("frame_%05d.txt", frame.getIndex()));
            Files.write(framePath, frame.getText().getBytes(StandardCharsets.UTF_8));
        }
    }

    public static void writeAnimationJson(AsciiAnimation animation, AsciiRenderPlan plan, Path outputPath, Path framesDirectory) throws IOException {
        ensureParent(outputPath);
        StringBuilder framesBuilder = new StringBuilder();
        for (int i = 0; i < animation.getFrames().size(); i++) {
            AsciiFrame frame = animation.getFrames().get(i);
            if (i > 0) {
                framesBuilder.append(",\n");
            }
            framesBuilder.append("    { \"file\": \"")
                    .append(escape(String.format("frame_%05d.txt", frame.getIndex())))
                    .append("\", \"durationMs\": ")
                    .append(frame.getDurationMillis())
                    .append(" }");
        }

        String json = "{\n" +
                "  \"source\": \"" + escape(plan.getSourcePath().toString()) + "\",\n" +
                "  \"type\": \"animation\",\n" +
                "  \"width\": " + animation.getWidth() + ",\n" +
                "  \"fps\": " + animation.getFps() + ",\n" +
                "  \"sampleEvery\": " + plan.getSampleEvery() + ",\n" +
                "  \"charset\": \"" + escape(plan.getCharset()) + "\",\n" +
                "  \"sortedCharset\": \"" + escape(animation.getSortedCharset()) + "\",\n" +
                "  \"fontName\": \"" + escape(plan.getFontName()) + "\",\n" +
                "  \"fontSize\": " + plan.getFontSize() + ",\n" +
                "  \"invert\": " + plan.isInvert() + ",\n" +
                "  \"threshold\": " + (plan.getThreshold() == null ? "null" : plan.getThreshold()) + ",\n" +
                "  \"framesDir\": \"" + escape(framesDirectory.toAbsolutePath().toString()) + "\",\n" +
                "  \"frameCount\": " + animation.getFrames().size() + ",\n" +
                "  \"frames\": [\n" +
                framesBuilder +
                "\n  ]\n" +
                "}\n";
        Files.write(outputPath, json.getBytes(StandardCharsets.UTF_8));
    }

    private static AsciiAnimation loadSingleImage(AsciiRenderPlan plan) throws IOException {
        BufferedImage image = ImageIO.read(plan.getSourcePath().toFile());
        if (image == null) {
            throw new IOException("Unsupported or unreadable image: " + plan.getSourcePath());
        }
        String sortedCharset = AsciiImageService.getSortedCharset(plan);
        String text = AsciiImageService.renderAscii(image, plan, sortedCharset);
        long frameDuration = plan.getFps() > 0 ? 1000L / plan.getFps() : DEFAULT_FRAME_DURATION_MS;
        return new AsciiAnimation(List.of(new AsciiFrame(0, frameDuration, text)), plan.getWidth(), plan.getFps(), sortedCharset);
    }

    private static AsciiAnimation loadGif(AsciiRenderPlan plan) throws IOException {
        List<AsciiFrame> frames = new ArrayList<>();
        String sortedCharset = AsciiImageService.getSortedCharset(plan);

        try (ImageInputStream stream = ImageIO.createImageInputStream(plan.getSourcePath().toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) {
                throw new IOException("GIF reader is not available.");
            }
            ImageReader reader = readers.next();
            reader.setInput(stream, false);

            int imageCount = reader.getNumImages(true);
            int frameIndex = 0;
            for (int imageIndex = 0; imageIndex < imageCount; imageIndex += Math.max(1, plan.getSampleEvery())) {
                BufferedImage frameImage = reader.read(imageIndex);
                long duration = 0;
                for (int sampledIndex = imageIndex; sampledIndex < Math.min(imageCount, imageIndex + Math.max(1, plan.getSampleEvery())); sampledIndex++) {
                    duration += readGifDelayMillis(reader, sampledIndex);
                }
                if (duration <= 0) {
                    duration = DEFAULT_FRAME_DURATION_MS;
                }
                String text = AsciiImageService.renderAscii(frameImage, plan, sortedCharset);
                frames.add(new AsciiFrame(frameIndex++, duration, text));
            }
            reader.dispose();
        }

        return new AsciiAnimation(frames, plan.getWidth(), plan.getFps(), sortedCharset);
    }

    private static AsciiAnimation loadVideo(AsciiRenderPlan plan) throws IOException {
        Path ffmpeg = resolveFfmpegPath(plan);
        Path tempDirectory = Files.createTempDirectory("ascii-video-frames");
        try {
            Path pattern = tempDirectory.resolve("frame_%05d.png");
            runFfmpeg(ffmpeg, plan, pattern);

            List<Path> frameFiles = Files.list(tempDirectory)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();

            if (frameFiles.isEmpty()) {
                throw new IOException("No frames were extracted from video.");
            }

            String sortedCharset = AsciiImageService.getSortedCharset(plan);
            List<AsciiFrame> frames = new ArrayList<>(frameFiles.size());
            long durationMillis = plan.getFps() > 0 ? Math.max(1L, Math.round(1000.0 / plan.getFps())) : DEFAULT_FRAME_DURATION_MS;
            int sampleEvery = Math.max(1, plan.getSampleEvery());
            int outputIndex = 0;
            for (int i = 0; i < frameFiles.size(); i += sampleEvery) {
                BufferedImage frameImage = ImageIO.read(frameFiles.get(i).toFile());
                if (frameImage == null) {
                    continue;
                }
                long sampledDuration = durationMillis * Math.min(sampleEvery, frameFiles.size() - i);
                String text = AsciiImageService.renderAscii(frameImage, plan, sortedCharset);
                frames.add(new AsciiFrame(outputIndex++, sampledDuration, text));
            }

            return new AsciiAnimation(frames, plan.getWidth(), plan.getFps(), sortedCharset);
        } finally {
            deleteDirectory(tempDirectory);
        }
    }

    private static long readGifDelayMillis(ImageReader reader, int imageIndex) throws IOException {
        Node root = reader.getImageMetadata(imageIndex).getAsTree("javax_imageio_gif_image_1.0");
        Node node = root.getFirstChild();
        while (node != null) {
            if ("GraphicControlExtension".equals(node.getNodeName())) {
                NamedNodeMap attributes = node.getAttributes();
                Node delayNode = attributes.getNamedItem("delayTime");
                if (delayNode != null) {
                    return Long.parseLong(delayNode.getNodeValue()) * 10L;
                }
            }
            node = node.getNextSibling();
        }
        return DEFAULT_FRAME_DURATION_MS;
    }

    private static Path resolveFfmpegPath(AsciiRenderPlan plan) throws IOException {
        String configured = plan.getFfmpegPath();
        if (configured == null || configured.isEmpty()) {
            return Path.of("ffmpeg");
        }
        Path path = Path.of(configured);
        if (Files.exists(path)) {
            return path;
        }
        if ("ffmpeg".equalsIgnoreCase(configured)) {
            return path;
        }
        throw new IOException("ffmpeg executable does not exist: " + configured);
    }

    private static void runFfmpeg(Path ffmpegPath, AsciiRenderPlan plan, Path outputPattern) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                ffmpegPath.toString(),
                "-y",
                "-loglevel", "error",
                "-i", plan.getSourcePath().toString(),
                "-vf", "fps=" + Math.max(1, plan.getFps()),
                outputPattern.toString()
        );
        processBuilder.redirectErrorStream(true);
        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new IOException("Cannot start ffmpeg. Set ffmpegPath to a valid ffmpeg.exe path or add ffmpeg to PATH.", e);
        }
        try {
            // Drain ffmpeg output before waiting, otherwise the process can block on a full pipe buffer.
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("ffmpeg failed with exit code " + exitCode + ": " + output);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("ffmpeg execution was interrupted.", e);
        }
    }

    private static void ensureDirectory(Path directory) throws IOException {
        Files.createDirectories(directory);
    }

    private static void ensureParent(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static void deleteDirectory(Path directory) throws IOException {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
