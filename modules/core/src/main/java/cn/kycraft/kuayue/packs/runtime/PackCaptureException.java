package cn.kycraft.kuayue.packs.runtime;

import java.io.IOException;
import java.util.Objects;

/** Classified failure while constructing one pack's frozen resource roots. */
public final class PackCaptureException extends IOException {
    public enum Kind {
        OWNER_INPUT,
        HOST_FAILURE
    }

    private final Kind kind;
    private final String packId;

    private PackCaptureException(Kind kind, String packId, String message, Throwable cause) {
        super(message, cause);
        this.kind = Objects.requireNonNull(kind, "kind");
        this.packId = Objects.requireNonNull(packId, "packId");
    }

    public static PackCaptureException ownerInput(String packId, String message, Throwable cause) {
        return new PackCaptureException(Kind.OWNER_INPUT, packId, message, cause);
    }

    public static PackCaptureException hostFailure(String packId, String message, Throwable cause) {
        return new PackCaptureException(Kind.HOST_FAILURE, packId, message, cause);
    }

    public Kind kind() {
        return kind;
    }

    public String packId() {
        return packId;
    }
}
