package cn.kycraft.kuayue.parts.bogeys;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BogeyTextureReferenceTest {

    private static final Path RESOURCES = Path.of("resources");
    private static final Pattern MAP_DIFFUSE =
            Pattern.compile("^map_Kd\\s+(?:kuayue:)?(\\S+)\\s*$", Pattern.MULTILINE);

    @Test
    void everyBogeyMaterialReferencesANonBlackTexture() throws IOException {
        List<Path> materials;
        try (var paths = Files.walk(RESOURCES)) {
            materials = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".mtl"))
                    .filter(path -> path.toString().contains("/models/block/bogey/"))
                    .toList();
        }
        assertFalse(materials.isEmpty());

        List<Path> textures;
        try (var paths = Files.walk(RESOURCES)) {
            textures = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().contains("/textures/"))
                    .filter(path -> path.toString().endsWith(".png"))
                    .toList();
        }

        List<String> failures = new ArrayList<>();
        int references = 0;
        for (Path materialFile : materials) {
            Path moduleRoot = moduleRoot(materialFile);
            var matcher = MAP_DIFFUSE.matcher(Files.readString(materialFile));
            while (matcher.find()) {
                references++;
                Path texture = moduleRoot.resolve("textures")
                        .resolve(matcher.group(1) + ".png");
                if (!Files.isRegularFile(texture)) {
                    Path suffix = Path.of("textures").resolve(matcher.group(1) + ".png");
                    texture = textures.stream()
                            .filter(path -> path.endsWith(suffix))
                            .findFirst()
                            .orElse(texture);
                }
                if (!Files.isRegularFile(texture)) {
                    failures.add(materialFile + " -> missing " + texture);
                    continue;
                }
                var image = ImageIO.read(texture.toFile());
                if (image == null) {
                    failures.add(materialFile + " -> unreadable " + texture);
                    continue;
                }
                assertNotNull(image);
                if (!hasVisibleColor(image)) {
                    failures.add(materialFile + " -> black texture " + texture);
                }
            }
        }

        assertTrue(references > 0);
        assertTrue(failures.isEmpty(), () -> String.join("\n", failures));
    }

    private static Path moduleRoot(Path materialFile) {
        Path current = materialFile.getParent();
        while (current != null && !current.getFileName().toString().equals("models")) {
            current = current.getParent();
        }
        assertNotNull(current, "No models directory for " + materialFile);
        return current.getParent();
    }

    private static boolean hasVisibleColor(java.awt.image.BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int alpha = argb >>> 24;
                int rgb = argb & 0x00FFFFFF;
                if (alpha != 0 && rgb != 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
