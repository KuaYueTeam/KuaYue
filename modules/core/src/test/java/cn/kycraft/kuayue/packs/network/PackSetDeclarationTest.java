package cn.kycraft.kuayue.packs.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackSetDeclarationTest {
    @Test
    void compatibilityIgnoresInputOrderButRejectsVersionChanges() {
        PackSetDeclaration first = new PackSetDeclaration(List.of(
                new PackSetDeclaration.Entry("alpha", "1.0.0", 1),
                new PackSetDeclaration.Entry("beta", "2.0.0", 1)
        ));
        PackSetDeclaration reordered = new PackSetDeclaration(List.of(
                new PackSetDeclaration.Entry("beta", "2.0.0", 1),
                new PackSetDeclaration.Entry("alpha", "1.0.0", 1)
        ));
        PackSetDeclaration changed = new PackSetDeclaration(List.of(
                new PackSetDeclaration.Entry("alpha", "1.0.1", 1),
                new PackSetDeclaration.Entry("beta", "2.0.0", 1)
        ));

        assertTrue(first.compatibleWith(reordered));
        assertFalse(first.compatibleWith(changed));
    }

    @Test
    void compatibilityRejectsShapeGenerationChangesAtTheSamePackVersion() {
        List<PackSetDeclaration.Entry> entries = List.of(
                new PackSetDeclaration.Entry("alpha", "1.0.0", 1)
        );
        PackSetDeclaration first = new PackSetDeclaration(entries, "a".repeat(64));
        PackSetDeclaration changed = new PackSetDeclaration(entries, "b".repeat(64));

        assertFalse(first.compatibleWith(changed));
    }

    @Test
    void shapeDigestRoundTripsThroughTheConfigurationCodec() {
        PackSetDeclaration declaration = new PackSetDeclaration(
                List.of(new PackSetDeclaration.Entry("alpha", "1.0.0", 1)),
                "a".repeat(64)
        );
        ByteBuf buffer = Unpooled.buffer();
        try {
            PackSetDeclarationCodec.STREAM_CODEC.encode(buffer, declaration);
            assertEquals(
                    declaration,
                    PackSetDeclarationCodec.STREAM_CODEC.decode(buffer)
            );
        } finally {
            buffer.release();
        }
    }
}
