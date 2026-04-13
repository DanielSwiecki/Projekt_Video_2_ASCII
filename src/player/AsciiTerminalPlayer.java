package player;

import model.AsciiFrame;

import java.util.List;

public class AsciiTerminalPlayer {

    public void play(List<AsciiFrame> frames, int fps) throws InterruptedException {
        if (frames == null || frames.isEmpty()) {
            System.out.println("Brak klatek do odtworzenia.");
            return;
        }

        long delay = 1000L / fps;

        for (AsciiFrame frame : frames) {
            clearScreen();
            System.out.print(frame.getText());
            Thread.sleep(delay);
        }
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}