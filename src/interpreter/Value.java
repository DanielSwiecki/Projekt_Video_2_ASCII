package interpreter;

public final class Value {
    public enum Kind {
        NUMBER,
        BOOLEAN,
        STRING,
        NULL
    }

    public static final Value NULL = new Value(Kind.NULL, null);

    private final Kind kind;
    private final Object raw;

    private Value(Kind kind, Object raw) {
        this.kind = kind;
        this.raw = raw;
    }

    public static Value of(double value) {
        return new Value(Kind.NUMBER, value);
    }

    public static Value of(boolean value) {
        return new Value(Kind.BOOLEAN, value);
    }

    public static Value of(String value) {
        return new Value(Kind.STRING, value);
    }

    public Kind getKind() {
        return kind;
    }

    public double asNumber() {
        if (kind == Kind.NUMBER) {
            return (Double) raw;
        }
        throw new IllegalStateException("Expected number but got " + kind);
    }

    public int asInt() {
        return (int) Math.round(asNumber());
    }

    public boolean asBoolean() {
        switch (kind) {
            case BOOLEAN:
                return (Boolean) raw;
            case NUMBER:
                return Math.abs((Double) raw) > 0.0000001;
            case STRING:
                return !((String) raw).isEmpty();
            case NULL:
                return false;
            default:
                throw new IllegalStateException("Unsupported kind " + kind);
        }
    }

    public String asString() {
        if (kind == Kind.STRING) {
            return (String) raw;
        }
        if (kind == Kind.NUMBER) {
            double value = (Double) raw;
            if (Math.rint(value) == value) {
                return Integer.toString((int) value);
            }
            return Double.toString(value);
        }
        if (kind == Kind.BOOLEAN) {
            return Boolean.toString((Boolean) raw);
        }
        return "null";
    }

    @Override
    public String toString() {
        return asString();
    }
}
