package player;

import java.nio.file.Path;

public class StartPlayer {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("Usage: StartPlayer <manifest.json> [--loop]");
        }

        Path manifestPath = Path.of(args[0]);
        boolean loop = args.length > 1 && "--loop".equalsIgnoreCase(args[1]);

        AsciiMovieManifest manifest = AsciiMovieManifestReader.read(manifestPath);
        AsciiTerminalPlayer player = new AsciiTerminalPlayer();
        player.play(manifest, loop);
    }
}
