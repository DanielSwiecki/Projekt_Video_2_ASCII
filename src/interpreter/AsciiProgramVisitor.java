package interpreter;

import SymbolTable.LocalSymbols;
import grammar.AsciiFlowBaseVisitor;
import grammar.AsciiFlowParser;
import org.antlr.v4.runtime.ParserRuleContext;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class AsciiProgramVisitor extends AsciiFlowBaseVisitor<Value> {
    private final LocalSymbols<Value> symbols = new LocalSymbols<>();
    private final AsciiRenderPlan plan = new AsciiRenderPlan();
    private final Path scriptDirectory;
    private AsciiAnimation cachedAnimation;
    private Path lastAnimationFramesDirectory;

    public AsciiProgramVisitor() {
        this(Paths.get("").toAbsolutePath().normalize());
    }

    public AsciiProgramVisitor(Path scriptDirectory) {
        this.scriptDirectory = scriptDirectory == null
                ? Paths.get("").toAbsolutePath().normalize()
                : scriptDirectory.toAbsolutePath().normalize();
    }

    public AsciiRenderPlan getPlan() {
        return plan;
    }

    @Override
    public Value visitProgram(AsciiFlowParser.ProgramContext ctx) {
        Value last = Value.NULL;
        for (AsciiFlowParser.StatementContext statement : ctx.statement()) {
            last = visit(statement);
        }
        return last;
    }

    @Override
    public Value visitBlock(AsciiFlowParser.BlockContext ctx) {
        symbols.enterScope();
        try {
            Value last = Value.NULL;
            for (AsciiFlowParser.StatementContext statement : ctx.statement()) {
                last = visit(statement);
            }
            return last;
        } finally {
            symbols.leaveScope();
        }
    }

    @Override
    public Value visitVarDecl(AsciiFlowParser.VarDeclContext ctx) {
        Value value = ctx.expr() == null ? Value.NULL : visit(ctx.expr());
        symbols.newSymbol(ctx.ID().getText(), value);
        return value;
    }

    @Override
    public Value visitAssignment(AsciiFlowParser.AssignmentContext ctx) {
        Value value = visit(ctx.expr());
        symbols.setSymbol(ctx.ID().getText(), value);
        return value;
    }

    @Override
    public Value visitSourceStmt(AsciiFlowParser.SourceStmtContext ctx) {
        plan.setSourcePath(resolvePath(visit(ctx.expr()).asString()));
        invalidateAnimationCache();
        return Value.NULL;
    }

    @Override
    public Value visitSampleStmt(AsciiFlowParser.SampleStmtContext ctx) {
        plan.setSampleEvery(requirePositiveInt(visit(ctx.expr()), ctx, "sample"));
        invalidateAnimationCache();
        return Value.NULL;
    }

    @Override
    public Value visitFpsStmt(AsciiFlowParser.FpsStmtContext ctx) {
        plan.setFps(requirePositiveInt(visit(ctx.expr()), ctx, "fps"));
        invalidateAnimationCache();
        return Value.NULL;
    }

    @Override
    public Value visitSetStmt(AsciiFlowParser.SetStmtContext ctx) {
        String property = ctx.ID().getText();
        Value value = visit(ctx.expr());

        switch (property) {
            case "width":
                plan.setWidth(requirePositiveInt(value, ctx, "width"));
                break;
            case "charset":
                String charset = value.asString();
                if (charset.isEmpty()) {
                    throw error(ctx, "charset cannot be empty");
                }
                plan.setCharset(charset);
                break;
            case "fontName":
                String fontName = value.asString();
                if (fontName.isEmpty()) {
                    throw error(ctx, "fontName cannot be empty");
                }
                plan.setFontName(fontName);
                break;
            case "fontSize":
                plan.setFontSize(requirePositiveInt(value, ctx, "fontSize"));
                break;
            case "ffmpegPath":
                String ffmpegPath = value.asString();
                if (ffmpegPath.isEmpty()) {
                    throw error(ctx, "ffmpegPath cannot be empty");
                }
                plan.setFfmpegPath(resolvePath(ffmpegPath).toString());
                break;
            case "invert":
                plan.setInvert(value.asBoolean());
                break;
            case "threshold":
                plan.setThreshold(requireByte(value, ctx, "threshold"));
                break;
            default:
                throw error(ctx, "unknown property '" + property + "'");
        }
        invalidateAnimationCache();
        return Value.NULL;
    }

    @Override
    public Value visitFilterStmt(AsciiFlowParser.FilterStmtContext ctx) {
        String filterName = ctx.ID().getText();
        List<AsciiFlowParser.ExprContext> args = ctx.argList() == null
                ? Collections.emptyList()
                : ctx.argList().expr();

        switch (filterName) {
            case "grayscale":
                if (!args.isEmpty()) {
                    throw error(ctx, "filter grayscale does not accept arguments");
                }
                plan.getFilters().add("grayscale");
                break;
            case "invert":
                if (!args.isEmpty()) {
                    throw error(ctx, "filter invert does not accept arguments");
                }
                plan.setInvert(true);
                plan.getFilters().add("invert");
                break;
            case "threshold":
                if (args.size() != 1) {
                    throw error(ctx, "filter threshold expects one argument");
                }
                int threshold = requireByte(visit(args.get(0)), ctx, "threshold");
                plan.setThreshold(threshold);
                plan.getFilters().add("threshold(" + threshold + ")");
                break;
            default:
                throw error(ctx, "unknown filter '" + filterName + "'");
        }

        invalidateAnimationCache();
        return Value.NULL;
    }

    @Override
    public Value visitExportStmt(AsciiFlowParser.ExportStmtContext ctx) {
        if (plan.getSourcePath() == null) {
            throw error(ctx, "source must be configured before export");
        }

        Path outputPath = resolvePath(visit(ctx.expr()).asString());

        try {
            if (ctx.ASCII() != null) {
                MediaType mediaType = AsciiAnimationService.detectMediaType(plan.getSourcePath());
                if (AsciiAnimationService.isAnimated(mediaType)) {
                    AsciiAnimation animation = getOrLoadAnimation();
                    if (looksLikeFilePath(outputPath)) {
                        throw error(ctx, "Animated ASCII export requires a directory path, not a single .txt file");
                    }
                    AsciiAnimationService.exportAnimationFrames(animation, outputPath);
                    lastAnimationFramesDirectory = outputPath.toAbsolutePath().normalize();
                } else {
                    String ascii = AsciiImageService.renderAscii(plan);
                    AsciiImageService.writeAsciiFile(ascii, outputPath);
                    AsciiImageService.writeHtmlPreview(ascii, plan, outputPath);
                }
            } else {
                MediaType mediaType = AsciiAnimationService.detectMediaType(plan.getSourcePath());
                if (AsciiAnimationService.isAnimated(mediaType)) {
                    if (lastAnimationFramesDirectory == null) {
                        throw error(ctx, "For animated media, export ascii to a directory before export json");
                    }
                    AsciiAnimation animation = getOrLoadAnimation();
                    AsciiAnimationService.writeAnimationJson(animation, plan, outputPath, lastAnimationFramesDirectory);
                } else {
                    AsciiImageService.writeJsonFile(plan, outputPath);
                }
            }
        } catch (IOException e) {
            throw error(ctx, "I/O error: " + e.getMessage());
        }

        return Value.NULL;
    }

    @Override
    public Value visitIfStmt(AsciiFlowParser.IfStmtContext ctx) {
        if (visit(ctx.expr()).asBoolean()) {
            return visit(ctx.block(0));
        }
        if (ctx.block().size() > 1) {
            return visit(ctx.block(1));
        }
        return Value.NULL;
    }

    @Override
    public Value visitForStmt(AsciiFlowParser.ForStmtContext ctx) {
        symbols.enterScope();
        try {
            if (ctx.forInit() != null) {
                visit(ctx.forInit());
            }

            while (ctx.expr() == null || visit(ctx.expr()).asBoolean()) {
                visit(ctx.block());
                if (ctx.assignment() != null) {
                    visit(ctx.assignment());
                }
            }

            return Value.NULL;
        } finally {
            symbols.leaveScope();
        }
    }

    @Override
    public Value visitForInit(AsciiFlowParser.ForInitContext ctx) {
        if (ctx.varDeclInline() != null) {
            Value value = ctx.varDeclInline().expr() == null ? Value.NULL : visit(ctx.varDeclInline().expr());
            symbols.newSymbol(ctx.varDeclInline().ID().getText(), value);
            return value;
        }
        return visit(ctx.assignment());
    }

    @Override
    public Value visitLogicalOr(AsciiFlowParser.LogicalOrContext ctx) {
        Value result = visit(ctx.logicalAnd(0));
        for (int i = 1; i < ctx.logicalAnd().size(); i++) {
            result = Value.of(result.asBoolean() || visit(ctx.logicalAnd(i)).asBoolean());
        }
        return result;
    }

    @Override
    public Value visitLogicalAnd(AsciiFlowParser.LogicalAndContext ctx) {
        Value result = visit(ctx.equality(0));
        for (int i = 1; i < ctx.equality().size(); i++) {
            result = Value.of(result.asBoolean() && visit(ctx.equality(i)).asBoolean());
        }
        return result;
    }

    @Override
    public Value visitEquality(AsciiFlowParser.EqualityContext ctx) {
        Value left = visit(ctx.comparison(0));
        for (int i = 1; i < ctx.comparison().size(); i++) {
            Value right = visit(ctx.comparison(i));
            String operator = ctx.getChild(2 * i - 1).getText();
            boolean result = "==".equals(operator) ? equalsValue(left, right) : !equalsValue(left, right);
            left = Value.of(result);
        }
        return left;
    }

    @Override
    public Value visitComparison(AsciiFlowParser.ComparisonContext ctx) {
        Value left = visit(ctx.addition(0));
        for (int i = 1; i < ctx.addition().size(); i++) {
            Value right = visit(ctx.addition(i));
            String operator = ctx.getChild(2 * i - 1).getText();
            double leftNumber = left.asNumber();
            double rightNumber = right.asNumber();
            boolean result;
            switch (operator) {
                case "<":
                    result = leftNumber < rightNumber;
                    break;
                case ">":
                    result = leftNumber > rightNumber;
                    break;
                case "<=":
                    result = leftNumber <= rightNumber;
                    break;
                case ">=":
                    result = leftNumber >= rightNumber;
                    break;
                default:
                    throw error(ctx, "unsupported comparison operator " + operator);
            }
            left = Value.of(result);
        }
        return left;
    }

    @Override
    public Value visitAddition(AsciiFlowParser.AdditionContext ctx) {
        Value left = visit(ctx.multiplication(0));
        for (int i = 1; i < ctx.multiplication().size(); i++) {
            Value right = visit(ctx.multiplication(i));
            String operator = ctx.getChild(2 * i - 1).getText();
            if ("+".equals(operator) && (left.getKind() == Value.Kind.STRING || right.getKind() == Value.Kind.STRING)) {
                left = Value.of(left.asString() + right.asString());
            } else if ("+".equals(operator)) {
                left = Value.of(left.asNumber() + right.asNumber());
            } else {
                left = Value.of(left.asNumber() - right.asNumber());
            }
        }
        return left;
    }

    @Override
    public Value visitMultiplication(AsciiFlowParser.MultiplicationContext ctx) {
        Value left = visit(ctx.unary(0));
        for (int i = 1; i < ctx.unary().size(); i++) {
            Value right = visit(ctx.unary(i));
            String operator = ctx.getChild(2 * i - 1).getText();
            switch (operator) {
                case "*":
                    left = Value.of(left.asNumber() * right.asNumber());
                    break;
                case "/":
                    left = Value.of(left.asNumber() / right.asNumber());
                    break;
                case "%":
                    left = Value.of(left.asNumber() % right.asNumber());
                    break;
                default:
                    throw error(ctx, "unsupported arithmetic operator " + operator);
            }
        }
        return left;
    }

    @Override
    public Value visitUnary(AsciiFlowParser.UnaryContext ctx) {
        if (ctx.primary() != null) {
            return visit(ctx.primary());
        }

        Value value = visit(ctx.unary());
        if (ctx.NOT() != null) {
            return Value.of(!value.asBoolean());
        }
        return Value.of(-value.asNumber());
    }

    @Override
    public Value visitPrimary(AsciiFlowParser.PrimaryContext ctx) {
        if (ctx.INT() != null) {
            return Value.of(Double.parseDouble(ctx.INT().getText()));
        }
        if (ctx.FLOAT() != null) {
            return Value.of(Double.parseDouble(ctx.FLOAT().getText()));
        }
        if (ctx.STRING() != null) {
            return Value.of(unquote(ctx.STRING().getText()));
        }
        if (ctx.TRUE() != null) {
            return Value.of(true);
        }
        if (ctx.FALSE() != null) {
            return Value.of(false);
        }
        if (ctx.ID() != null) {
            return symbols.getSymbol(ctx.ID().getText());
        }
        return visit(ctx.expr());
    }

    private int requirePositiveInt(Value value, ParserRuleContext ctx, String name) {
        int intValue = value.asInt();
        if (intValue <= 0) {
            throw error(ctx, name + " must be greater than zero");
        }
        return intValue;
    }

    private int requireByte(Value value, ParserRuleContext ctx, String name) {
        int intValue = value.asInt();
        if (intValue < 0 || intValue > 255) {
            throw error(ctx, name + " must be in range 0..255");
        }
        return intValue;
    }

    private boolean equalsValue(Value left, Value right) {
        if (left.getKind() == Value.Kind.NUMBER && right.getKind() == Value.Kind.NUMBER) {
            return Math.abs(left.asNumber() - right.asNumber()) < 0.0000001;
        }
        return left.asString().equals(right.asString());
    }

    private RuntimeException error(ParserRuleContext ctx, String message) {
        return new RuntimeException("Line " + ctx.getStart().getLine() + ": " + message);
    }

    private void invalidateAnimationCache() {
        cachedAnimation = null;
        lastAnimationFramesDirectory = null;
    }

    private AsciiAnimation getOrLoadAnimation() throws IOException {
        if (cachedAnimation == null) {
            cachedAnimation = AsciiAnimationService.loadAnimation(plan);
        }
        return cachedAnimation;
    }

    private boolean looksLikeFilePath(Path path) {
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
        return fileName.contains(".");
    }

    private String unquote(String text) {
        String body = text.substring(1, text.length() - 1);
        StringBuilder builder = new StringBuilder(body.length());
        for (int i = 0; i < body.length(); i++) {
            char current = body.charAt(i);
            if (current != '\\') {
                builder.append(current);
                continue;
            }

            if (i + 1 >= body.length()) {
                builder.append('\\');
                continue;
            }

            char next = body.charAt(++i);
            switch (next) {
                case '\\':
                    builder.append('\\');
                    break;
                case '"':
                    builder.append('"');
                    break;
                default:
                    builder.append('\\').append(next);
                    break;
            }
        }
        return builder.toString();
    }

    private Path resolvePath(String rawPath) {
        Path candidate = Paths.get(rawPath);
        if (candidate.isAbsolute()) {
            return candidate.normalize();
        }
        return scriptDirectory.resolve(candidate).normalize();
    }
}
