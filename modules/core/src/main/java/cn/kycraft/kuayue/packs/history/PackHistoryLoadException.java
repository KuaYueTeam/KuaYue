package cn.kycraft.kuayue.packs.history;

public final class PackHistoryLoadException extends IllegalArgumentException {
    public enum Reason {
        CORRUPT,
        UNSUPPORTED_SCHEMA
    }

    private final Reason reason;

    public PackHistoryLoadException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public PackHistoryLoadException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
