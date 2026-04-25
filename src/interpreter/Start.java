package interpreter;

import grammar.AsciiFlowLexer;
import grammar.AsciiFlowParser;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.nio.file.Files;
import java.nio.file.Path;

public class Start {
    public static void main(String[] args) {
        String scriptPath = args.length > 0 ? args[0] : "cat_video.first";
        CharStream input;
        Path scriptFilePath;
        try {
            Path path = Path.of(scriptPath);
            if (!Files.exists(path)) {
                throw new IllegalArgumentException(
                        "Script file does not exist: " + path.toAbsolutePath() + System.lineSeparator()
                                + "Usage: java interpreter.Start <script.first>"
                );
            }
            scriptFilePath = path.toAbsolutePath().normalize();
            input = CharStreams.fromFileName(scriptPath);
        } catch (Exception e) {
            throw new RuntimeException("Cannot read script file: " + scriptPath, e);
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

        Path scriptDirectory = scriptFilePath.getParent();
        AsciiProgramVisitor visitor = new AsciiProgramVisitor(scriptDirectory);
        visitor.visit(parser.program());
    }
}
