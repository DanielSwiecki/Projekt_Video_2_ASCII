package model;

public class AsciiFrame {
    private final String text;
    private final int index;

    public AsciiFrame(String text, int index) {
        this.text = text;
        this.index = index;
    }

    public String getText() {
        return text;
    }

    public int getIndex() {
        return index;
    }
}