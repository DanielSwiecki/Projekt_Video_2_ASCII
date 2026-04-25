package player;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AsciiMovieManifestReader {
    private static final Pattern STRING_FIELD = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern NUMBER_FIELD = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\\d+)");
    private static final Pattern FRAME_PATTERN = Pattern.compile("\\{\\s*\"file\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"durationMs\"\\s*:\\s*(\\d+)\\s*}");

    private AsciiMovieManifestReader() {
    }

    public static AsciiMovieManifest read(Path manifestPath) throws IOException {
        String content = readText(manifestPath);

        String framesDir = findStringField(content, "framesDir");
        int width = findIntField(content, "width", 80);
        int fps = findIntField(content, "fps", 12);
        String fontName = findStringField(content, "fontName");
        int fontSize = findIntField(content, "fontSize", 16);

        List<AsciiMovieManifest.FrameInfo> frames = new ArrayList<>();
        Matcher frameMatcher = FRAME_PATTERN.matcher(content);
        while (frameMatcher.find()) {
            frames.add(new AsciiMovieManifest.FrameInfo(frameMatcher.group(1), Long.parseLong(frameMatcher.group(2))));
        }

        if (frames.isEmpty()) {
            throw new IOException("Manifest does not contain any frames: " + manifestPath);
        }

        return new AsciiMovieManifest(Path.of(framesDir), frames, width, fps, fontName, fontSize);
    }

    private static String readText(Path manifestPath) throws IOException {
        byte[] bytes = Files.readAllBytes(manifestPath);
        if (bytes.length >= 2) {
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;
            if (first == 0xFF && second == 0xFE) {
                return new String(bytes, StandardCharsets.UTF_16LE);
            }
            if (first == 0xFE && second == 0xFF) {
                return new String(bytes, StandardCharsets.UTF_16BE);
            }
        }
        if (bytes.length >= 3) {
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;
            int third = bytes[2] & 0xFF;
            if (first == 0xEF && second == 0xBB && third == 0xBF) {
                return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
            }
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static String findStringField(String content, String fieldName) throws IOException {
        Matcher matcher = STRING_FIELD.matcher(content);
        while (matcher.find()) {
            if (fieldName.equals(matcher.group(1))) {
                return matcher.group(2).replace("\\\\", "\\").replace("\\\"", "\"");
            }
        }
        throw new IOException("Field '" + fieldName + "' was not found in manifest.");
    }

    private static int findIntField(String content, String fieldName, int defaultValue) {
        Matcher matcher = NUMBER_FIELD.matcher(content);
        while (matcher.find()) {
            if (fieldName.equals(matcher.group(1))) {
                return Integer.parseInt(matcher.group(2));
            }
        }
        return defaultValue;
    }
}
