package interpreter;

public class AsciiFrame {
    private final int index;
    private final long durationMillis;
    private final String text;

    public AsciiFrame(int index, long durationMillis, String text) {
        this.index = index;
        this.durationMillis = durationMillis;
        this.text = text;
    }

    public int getIndex() {
        return index;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public String getText() {
        return text;
    }
}
