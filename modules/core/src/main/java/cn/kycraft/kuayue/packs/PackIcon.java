package cn.kycraft.kuayue.packs;

import java.io.Serializable;
import java.util.Objects;

/** Frozen image bytes from the pack root's fixed {@value #FILE_NAME} entry. */
public record PackIcon(byte[] bytes) implements Serializable {
    public static final String FILE_NAME = "icon.png";

    public PackIcon {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length == 0) {
            throw new IllegalArgumentException("Pack icon must not be empty");
        }
        bytes = bytes.clone();
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
