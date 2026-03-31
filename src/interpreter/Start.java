package interpreter;

import grammar.AsciiFlowLexer;
import grammar.AsciiFlowParser;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class Start {
    public static void main(String[] args) {
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
    }
}
