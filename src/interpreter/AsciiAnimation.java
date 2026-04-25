package interpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AsciiAnimation {
    private final List<AsciiFrame> frames;
    private final int width;
    private final int fps;
    private final String sortedCharset;

    public AsciiAnimation(List<AsciiFrame> frames, int width, int fps, String sortedCharset) {
        this.frames = new ArrayList<>(frames);
        this.width = width;
        this.fps = fps;
        this.sortedCharset = sortedCharset;
    }

    public List<AsciiFrame> getFrames() {
        return Collections.unmodifiableList(frames);
    }

    public int getWidth() {
        return width;
    }

    public int getFps() {
        return fps;
    }

    public String getSortedCharset() {
        return sortedCharset;
    }
}
