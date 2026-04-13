package media;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FfmpegVideoFrameSource {

    public List<Path> extractFrames(String inputVideoPath, int fps) throws IOException, InterruptedException {
        Path framesDir = Paths.get("tmp", "frames");

        recreateDirectory(framesDir);

        List<String> command = List.of(
                "C:\\ffmpeg\\bin\\ffmpeg.exe",
                "-y",
                "-i", inputVideoPath,
                "-vf", "fps=" + fps,
                framesDir.resolve("frame_%05d.png").toString()
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        process.getInputStream().transferTo(System.out);

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg zakończył się błędem. Kod: " + exitCode);
        }

        List<Path> frames = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(framesDir, "*.png")) {
            for (Path path : stream) {
                frames.add(path);
            }
        }

        frames.sort(Comparator.naturalOrder());
        return frames;
    }

    private void recreateDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (var walk = Files.walk(dir)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
        }
        Files.createDirectories(dir);
    }
}