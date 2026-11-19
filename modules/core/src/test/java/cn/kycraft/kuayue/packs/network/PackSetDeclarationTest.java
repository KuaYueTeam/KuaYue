package cn.kycraft.kuayue.packs.network;

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
}
