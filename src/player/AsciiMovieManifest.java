package player;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AsciiMovieManifest {
    private final Path framesDirectory;
    private final List<FrameInfo> frames;
    private final int width;
    private final int fps;
    private final String fontName;
    private final int fontSize;

    public AsciiMovieManifest(Path framesDirectory, List<FrameInfo> frames, int width, int fps, String fontName, int fontSize) {
        this.framesDirectory = framesDirectory;
        this.frames = new ArrayList<>(frames);
        this.width = width;
        this.fps = fps;
        this.fontName = fontName;
        this.fontSize = fontSize;
    }

    public Path getFramesDirectory() {
        return framesDirectory;
    }

    public List<FrameInfo> getFrames() {
        return Collections.unmodifiableList(frames);
    }

    public int getWidth() {
        return width;
    }

    public int getFps() {
        return fps;
    }

    public String getFontName() {
        return fontName;
    }

    public int getFontSize() {
        return fontSize;
    }

    public static class FrameInfo {
        private final String fileName;
        private final long durationMillis;

        public FrameInfo(String fileName, long durationMillis) {
            this.fileName = fileName;
            this.durationMillis = durationMillis;
        }

        public String getFileName() {
            return fileName;
        }

        public long getDurationMillis() {
            return durationMillis;
        }
    }
}
