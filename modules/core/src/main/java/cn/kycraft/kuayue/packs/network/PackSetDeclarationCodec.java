package cn.kycraft.kuayue.packs.network;

import cn.kycraft.kuayue.packs.PackManifest;
import cn.kycraft.kuayue.packs.SemanticVersion;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Bounded wire codec for the canonical pack-set declaration. */
public final class PackSetDeclarationCodec {
    public static final StreamCodec<ByteBuf, PackSetDeclaration> STREAM_CODEC =
            StreamCodec.of(PackSetDeclarationCodec::write, PackSetDeclarationCodec::read);

    private PackSetDeclarationCodec() {
    }

    private static void write(ByteBuf buffer, PackSetDeclaration declaration) {
        buffer.writeShort(declaration.entries().size());
        for (PackSetDeclaration.Entry entry : declaration.entries()) {
            writeUtf(buffer, entry.id());
            writeUtf(buffer, entry.version());
            buffer.writeInt(entry.formatVersion());
        }
    }

    private static PackSetDeclaration read(ByteBuf buffer) {
        int count = buffer.readUnsignedShort();
        if (count > PackSetDeclaration.MAX_ENTRIES) {
            throw new IllegalArgumentException("Too many pack entries");
        }
        List<PackSetDeclaration.Entry> entries = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            String id = readUtf(buffer, 96);
            String version = readUtf(buffer, 96);
            SemanticVersion.parse(version);
            int format = buffer.readInt();
            if (format != PackManifest.FORMAT_VERSION) {
                throw new IllegalArgumentException("Unsupported pack format: " + format);
            }
            entries.add(new PackSetDeclaration.Entry(id, version, format));
        }
        return new PackSetDeclaration(entries);
    }

    private static void writeUtf(ByteBuf buffer, String value) {
        if (value.isEmpty() || value.length() > 96) {
            throw new IllegalArgumentException("Pack declaration string length");
        }
        buffer.writeShort(value.length());
        buffer.writeCharSequence(value, StandardCharsets.UTF_8);
    }

    private static String readUtf(ByteBuf buffer, int maximum) {
        int length = buffer.readUnsignedShort();
        if (length < 1 || length > maximum) {
            throw new IllegalArgumentException("Invalid string length");
        }
        return buffer.readCharSequence(length, StandardCharsets.UTF_8).toString();
    }
}
