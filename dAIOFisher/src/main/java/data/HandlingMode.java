package data;

public enum HandlingMode {
    DROP,
    BANK,
    COOK,
    COOKnBANK,
    STACK,
    NOTE,
    COOKnNOTE;

    @Override
    public String toString() {
        return switch (this) {
            case COOKnBANK -> "Cook & Bank";
            case COOKnNOTE -> "Cook & Note";
            default -> name().charAt(0) + name().substring(1).toLowerCase();
        };
    }
}