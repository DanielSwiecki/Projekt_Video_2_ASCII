package interpreter;

import grammar.AsciiFlowLexer;
import grammar.AsciiFlowParser;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

// importy do testów:
import media.FfmpegVideoFrameSource;

import model.AsciiFrame;
import model.PlaybackPlan;
import player.AsciiTerminalPlayer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Start {
    public static void main(String[] args) {


        // OSTATECZNE
        CharStream input;
        try {
            String scriptPath = args.length > 0 ? args[0] : "tangiro.first";
            input = CharStreams.fromFileName(scriptPath);
        } catch (Exception e) {
            throw new RuntimeException("Cannot read script file.", e);
        }

        AsciiFlowLexer lexer = new AsciiFlowLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        AsciiFlowParser parser = new AsciiFlowParser(tokens);

        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
                                    String msg, RecognitionException e) {
                throw new RuntimeException("Syntax error at " + line + ":" + charPositionInLine + " - " + msg, e);
            }
        });

        AsciiProgramVisitor visitor = new AsciiProgramVisitor();
        visitor.visit(parser.program());

        /* TEST 1
        try {
            FfmpegVideoFrameSource frameSource = new FfmpegVideoFrameSource();
            frameSource.extractFrames("C:\\Users\\Gabrysia\\Desktop\\studia\\sem 1 mgr\\antlr_lab\\input.mp4", 8);
            System.out.println("Klatki wyciągnięte poprawnie.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // TEST 2
        try {
            AsciiRenderPlan renderPlan = new AsciiRenderPlan();
            renderPlan.setWidth(120);
            renderPlan.setCharset("@%#*+=-:. ");
            renderPlan.setInvert(false);

            PlaybackPlan playbackPlan = new PlaybackPlan();
            playbackPlan.setFps(8);

            List<AsciiFrame> frames = new ArrayList<>();

            for (int i = 1; i <= 20; i++) {
                String fileName = String.format("tmp/frames/frame_%05d.png", i);
                BufferedImage image = ImageIO.read(Paths.get(fileName).toFile());

                if (image == null) {
                    System.out.println("Nie udało się wczytać: " + fileName);
                    continue;
                }

                String ascii = AsciiRenderService.renderAscii(image, renderPlan);
                frames.add(new AsciiFrame(ascii, i));
            }

            AsciiTerminalPlayer player = new AsciiTerminalPlayer();
            player.play(frames, playbackPlan.getFps());

        } catch (Exception e) {
            e.printStackTrace();
        }

        // TEST 3
        try {
            AsciiRenderPlan renderPlan = new AsciiRenderPlan();
            renderPlan.setWidth(120);
            renderPlan.setCharset("@%#*+=-:. ");
            renderPlan.setInvert(false);

            PlaybackPlan playbackPlan = new PlaybackPlan();
            playbackPlan.setSourcePath("C:\\Users\\Gabrysia\\Desktop\\studia\\sem 1 mgr\\antlr_lab\\input.mp4");
            playbackPlan.setFps(8);
            playbackPlan.setSampleEvery(2);
            playbackPlan.setWidth(120);
            playbackPlan.setCharset("@%#*+=-:. ");
            playbackPlan.setInvert(false);
            playbackPlan.setPlay(true);

            VideoAsciiPipeline pipeline = new VideoAsciiPipeline(renderPlan, playbackPlan);
            pipeline.run(playbackPlan);

        } catch (Exception e) {
            e.printStackTrace();
        }

         */
    }
}
