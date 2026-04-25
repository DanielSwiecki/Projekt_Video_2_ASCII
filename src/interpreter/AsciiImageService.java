package interpreter;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImageOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AsciiImageService {
    private static final int MIN_FONT_SIZE = 8;

    private AsciiImageService() {
    }

    public static String renderAscii(AsciiRenderPlan plan) throws IOException {
        if (plan.getSourcePath() == null) {
            throw new IllegalStateException("Source image is not configured.");
        }

        BufferedImage image = ImageIO.read(plan.getSourcePath().toFile());
        if (image == null) {
            throw new IOException("Unsupported or unreadable image: " + plan.getSourcePath());
        }

        return renderAscii(image, plan, getSortedCharset(plan));
    }

    public static String renderAscii(BufferedImage image, AsciiRenderPlan plan, String sortedCharset) {
        int targetWidth = Math.max(1, plan.getWidth());
        int targetHeight = Math.max(1, (int) Math.round(image.getHeight() * (targetWidth / (double) image.getWidth()) * 0.5));
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();

        StringBuilder builder = new StringBuilder(targetHeight * (targetWidth + 1));
        String charset = sortedCharset;
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
                if (plan.isInvert()) {
                    index = charset.length() - 1 - index;
                }

                builder.append(charset.charAt(index));
            }
            builder.append(System.lineSeparator());
        }

        return builder.toString();
    }

    public static String getSortedCharset(AsciiRenderPlan plan) {
        return normalizeCharset(plan);
    }

    public static void writeAsciiFile(String ascii, Path outputPath) throws IOException {
        ensureParent(outputPath);
        Files.write(outputPath, ascii.getBytes(StandardCharsets.UTF_8));
    }

    public static void writeHtmlPreview(String ascii, AsciiRenderPlan plan, Path asciiPath) throws IOException {
        ensureParent(asciiPath);
        Font font = resolveFont(plan);
        Path previewPath = buildPreviewPath(asciiPath);
        String html = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <title>ASCII Preview</title>\n" +
                "  <style>\n" +
                "    body { background: #111; color: #f1f1f1; margin: 0; padding: 20px; }\n" +
                "    pre { margin: 0; white-space: pre; font-family: '" + escapeHtml(font.getFamily()) +
                "', 'Cascadia Mono', 'Courier New', monospace; font-size: " + plan.getFontSize() +
                "px; line-height: 1; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<pre>" + escapeHtml(ascii) + "</pre>\n" +
                "</body>\n" +
                "</html>\n";
        Files.write(previewPath, html.getBytes(StandardCharsets.UTF_8));
    }

    public static void writeJsonFile(AsciiRenderPlan plan, Path outputPath) throws IOException {
        ensureParent(outputPath);
        Font font = resolveFont(plan);
        String sortedCharset = normalizeCharset(plan);
        String json = "{\n" +
                "  \"source\": \"" + escape(plan.getSourcePath().toString()) + "\",\n" +
                "  \"width\": " + plan.getWidth() + ",\n" +
                "  \"sampleEvery\": " + plan.getSampleEvery() + ",\n" +
                "  \"fps\": " + plan.getFps() + ",\n" +
                "  \"charset\": \"" + escape(plan.getCharset()) + "\",\n" +
                "  \"sortedCharset\": \"" + escape(sortedCharset) + "\",\n" +
                "  \"fontName\": \"" + escape(font.getFamily()) + "\",\n" +
                "  \"fontSize\": " + plan.getFontSize() + ",\n" +
                "  \"ffmpegPath\": \"" + escape(plan.getFfmpegPath()) + "\",\n" +
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

    private static Path buildPreviewPath(Path asciiPath) {
        String fileName = asciiPath.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        String baseName = extensionIndex >= 0 ? fileName.substring(0, extensionIndex) : fileName;
        String previewName = baseName + ".preview.html";
        return asciiPath.resolveSibling(previewName);
    }

    private static String normalizeCharset(AsciiRenderPlan plan) {
        String rawCharset = plan.getCharset();
        if (rawCharset == null || rawCharset.isEmpty()) {
            throw new IllegalStateException("Charset cannot be empty.");
        }

        Set<Character> uniqueCharacters = new LinkedHashSet<>();
        for (int i = 0; i < rawCharset.length(); i++) {
            uniqueCharacters.add(rawCharset.charAt(i));
        }

        List<WeightedCharacter> weightedCharacters = new ArrayList<>();
        Font font = resolveFont(plan);
        int order = 0;
        for (Character character : uniqueCharacters) {
            weightedCharacters.add(new WeightedCharacter(character, calculateInkCoverage(character, font), order++));
        }

        weightedCharacters.sort((left, right) -> {
            int coverageCompare = Double.compare(right.coverage, left.coverage);
            if (coverageCompare != 0) {
                return coverageCompare;
            }
            return Integer.compare(left.originalOrder, right.originalOrder);
        });

        StringBuilder sorted = new StringBuilder(weightedCharacters.size());
        for (WeightedCharacter weightedCharacter : weightedCharacters) {
            sorted.append(weightedCharacter.character);
        }
        return sorted.toString();
    }

    private static double calculateInkCoverage(char character, Font font) {
        BufferedImage canvas = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        graphics.setFont(font);
        graphics.setColor(Color.BLACK);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        FontMetrics metrics = graphics.getFontMetrics();
        int x = Math.max(0, (canvas.getWidth() - metrics.charWidth(character)) / 2);
        int y = Math.max(metrics.getAscent(), (canvas.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
        graphics.drawString(String.valueOf(character), x, y);
        graphics.dispose();

        long totalDarkness = 0;
        for (int yIndex = 0; yIndex < canvas.getHeight(); yIndex++) {
            for (int xIndex = 0; xIndex < canvas.getWidth(); xIndex++) {
                int rgb = canvas.getRGB(xIndex, yIndex);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                int brightness = (red + green + blue) / 3;
                totalDarkness += 255 - brightness;
            }
        }
        return totalDarkness;
    }

    private static Font resolveFont(AsciiRenderPlan plan) {
        String requestedName = plan.getFontName();
        int size = Math.max(MIN_FONT_SIZE, plan.getFontSize());
        Font requestedFont = new Font(requestedName == null || requestedName.isEmpty() ? Font.MONOSPACED : requestedName, Font.PLAIN, size);
        if (isMonospaced(requestedFont)) {
            return requestedFont;
        }
        return new Font(Font.MONOSPACED, Font.PLAIN, size);
    }

    private static boolean isMonospaced(Font font) {
        BufferedImage canvas = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics();
        int widthI = metrics.charWidth('i');
        int widthW = metrics.charWidth('W');
        int widthSpace = metrics.charWidth(' ');
        graphics.dispose();
        return widthI == widthW && widthW == widthSpace;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static final class WeightedCharacter {
        private final char character;
        private final double coverage;
        private final int originalOrder;

        private WeightedCharacter(char character, double coverage, int originalOrder) {
            this.character = character;
            this.coverage = coverage;
            this.originalOrder = originalOrder;
        }
    }
}
