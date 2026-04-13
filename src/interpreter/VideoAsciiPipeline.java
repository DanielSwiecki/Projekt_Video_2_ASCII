package interpreter;

import media.FfmpegVideoFrameSource;
import model.AsciiFrame;
import model.PlaybackPlan;
import player.AsciiTerminalPlayer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class VideoAsciiPipeline {
    private final AsciiRenderPlan renderPlan;
    private final PlaybackPlan playbackPlan;

    public VideoAsciiPipeline(AsciiRenderPlan renderPlan, PlaybackPlan playbackPlan) {
        this.renderPlan = renderPlan;
        this.playbackPlan = playbackPlan;
    }

    public void run(PlaybackPlan playbackPlan) throws Exception {
        FfmpegVideoFrameSource frameSource = new FfmpegVideoFrameSource();
        AsciiRenderService renderService = new AsciiRenderService();
        AsciiTerminalPlayer player = new AsciiTerminalPlayer();

        List<AsciiFrame> asciiFrames = new ArrayList<>();
        AsciiRenderPlan renderPlan = new AsciiRenderPlan();
        renderPlan.setWidth(playbackPlan.getWidth());
        renderPlan.setCharset(playbackPlan.getCharset());
        renderPlan.setInvert(playbackPlan.isInvert());
        renderPlan.setThreshold(playbackPlan.getThreshold());
        renderPlan.setSampleEvery(playbackPlan.getSampleEvery());
        renderPlan.setFps(playbackPlan.getFps());

        List<Path> allFrames = frameSource.extractFrames(playbackPlan.getSourcePath(), playbackPlan.getFps());

        int asciiIndex = 0;

        for (int i = 0; i < allFrames.size(); i++) {
            if (i % playbackPlan.getSampleEvery() != 0) {
                continue;
            }

            BufferedImage image = ImageIO.read(allFrames.get(i).toFile());
            String ascii = renderService.renderAscii(image, renderPlan);
            asciiFrames.add(new AsciiFrame(ascii, asciiIndex++));
        }

        if (playbackPlan.isPlay()) {
            player.play(asciiFrames, playbackPlan.getFps());
        } else {
            System.out.println("Plan nie ma ustawionego play=true");
        }
    }
}