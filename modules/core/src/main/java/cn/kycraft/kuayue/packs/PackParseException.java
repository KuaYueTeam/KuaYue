package cn.kycraft.kuayue.packs;

/**
 * A format error with a stable diagnostic code and JSON pointer.
 */
public final class PackParseException extends IllegalArgumentException {
    private final String code;
    private final String jsonPointer;

    public PackParseException(String code, String jsonPointer, String message) {
        super(message);
        this.code = code;
        this.jsonPointer = jsonPointer;
    }

    public PackParseException(String code, String jsonPointer, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.jsonPointer = jsonPointer;
    }

    public String code() {
        return code;
    }

    public String jsonPointer() {
        return jsonPointer;
    }
}
