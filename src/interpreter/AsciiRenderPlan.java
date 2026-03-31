package interpreter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AsciiRenderPlan {
    private Path sourcePath;
    private int sampleEvery = 1;
    private int fps = 12;
    private int width = 120;
    private String charset = "@%#*+=-:. ";
    private boolean invert = false;
    private Integer threshold = null;
    private final List<String> filters = new ArrayList<>();

    public Path getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(Path sourcePath) {
        this.sourcePath = sourcePath;
    }

    public int getSampleEvery() {
        return sampleEvery;
    }

    public void setSampleEvery(int sampleEvery) {
        this.sampleEvery = sampleEvery;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public boolean isInvert() {
        return invert;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    public Integer getThreshold() {
        return threshold;
    }

    public void setThreshold(Integer threshold) {
        this.threshold = threshold;
    }

    public List<String> getFilters() {
        return filters;
    }
}
