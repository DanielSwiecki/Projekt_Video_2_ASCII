package player;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class AsciiTerminalPlayer {
    private static final String CLEAR_SCREEN = "\u001b[H\u001b[2J";
    private static final String HIDE_CURSOR = "\u001b[?25l";
    private static final String SHOW_CURSOR = "\u001b[?25h";

    public void play(AsciiMovieManifest manifest, boolean loop) throws IOException {
        System.out.print(HIDE_CURSOR);
        System.out.flush();
        try {
            do {
                for (AsciiMovieManifest.FrameInfo frame : manifest.getFrames()) {
                    String content = Files.readString(manifest.getFramesDirectory().resolve(frame.getFileName()), StandardCharsets.UTF_8);
                    long startedAt = System.currentTimeMillis();
                    System.out.print(CLEAR_SCREEN);
                    System.out.print(content);
                    System.out.flush();

                    long renderElapsed = System.currentTimeMillis() - startedAt;
                    long sleepMillis = Math.max(1L, frame.getDurationMillis() - renderElapsed);
                    try {
                        Thread.sleep(sleepMillis);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            } while (loop);
        } finally {
            System.out.print(SHOW_CURSOR);
            System.out.flush();
        }
    }
}
