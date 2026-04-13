package interpreter;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AsciiRenderService {
    public AsciiRenderService() {
    }

    public static String renderAscii(BufferedImage image, AsciiRenderPlan plan) throws IOException {
        if (image == null) {
            throw new IOException("Image is null.");
        }

        int targetWidth = Math.max(1, plan.getWidth());
        int targetHeight = Math.max(1, (int) Math.round(image.getHeight() * (targetWidth / (double) image.getWidth()) * 0.5));
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();

        StringBuilder builder = new StringBuilder(targetHeight * (targetWidth + 1));
        String charset = plan.getCharset();
        if (charset == null || charset.isEmpty()) {
            throw new IllegalStateException("Charset cannot be empty.");
        }

        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                int rgb = scaled.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int brightness = (int) Math.round(0.2126 * r + 0.7152 * g + 0.0722 * b);

                if (plan.getThreshold() != null) {
                    brightness = brightness >= plan.getThreshold() ? 255 : 0;
                }

                int index = brightness * (charset.length() - 1) / 255;
                if (!plan.isInvert()) {
                    index = charset.length() - 1 - index;
                }

                builder.append(charset.charAt(index));
            }
            builder.append(System.lineSeparator());
        }

        return builder.toString();
    }

    public static String renderAscii(AsciiRenderPlan plan) throws IOException {
        if (plan.getSourcePath() == null) {
            throw new IllegalStateException("Source image is not configured.");
        }

        BufferedImage image = ImageIO.read(plan.getSourcePath().toFile());
        if (image == null) {
            throw new IOException("Unsupported or unreadable image: " + plan.getSourcePath());
        }

        return renderAscii(image, plan);
    }

    public static void writeAsciiFile(String ascii, Path outputPath) throws IOException {
        ensureParent(outputPath);
        Files.write(outputPath, ascii.getBytes(StandardCharsets.UTF_8));
    }

    public static void writeJsonFile(AsciiRenderPlan plan, Path outputPath) throws IOException {
        ensureParent(outputPath);
        String json = "{\n" +
                "  \"source\": \"" + escape(plan.getSourcePath().toString()) + "\",\n" +
                "  \"width\": " + plan.getWidth() + ",\n" +
                "  \"sampleEvery\": " + plan.getSampleEvery() + ",\n" +
                "  \"fps\": " + plan.getFps() + ",\n" +
                "  \"charset\": \"" + escape(plan.getCharset()) + "\",\n" +
                "  \"invert\": " + plan.isInvert() + ",\n" +
                "  \"threshold\": " + (plan.getThreshold() == null ? "null" : plan.getThreshold()) + ",\n" +
                "  \"filters\": " + toJsonArray(plan) + "\n" +
                "}\n";
        Files.write(outputPath, json.getBytes(StandardCharsets.UTF_8));
    }

    private static String toJsonArray(AsciiRenderPlan plan) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < plan.getFilters().size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append('"').append(escape(plan.getFilters().get(i))).append('"');
        }
        builder.append(']');
        return builder.toString();
    }

    private static void ensureParent(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
