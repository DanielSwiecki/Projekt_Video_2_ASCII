package model;

import java.util.ArrayList;
import java.util.List;

public class PlaybackPlan {
    private String sourcePath;
    private int width = 120;
    private int sampleEvery = 1;
    private int fps = 12;
    private boolean play = false;
    private String charset = "@%#*+=-:. ";
    private boolean invert = false;
    private Integer threshold = null;
    private final List<String> filters = new ArrayList<>();

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
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

    public boolean isPlay() {
        return play;
    }

    public void setPlay(boolean play) {
        this.play = play;
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