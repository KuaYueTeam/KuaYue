package cn.kycraft.kuayue.parts.bogeys;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BogeyProxyConfigTest {

    private static final Path BOGEY_RESOURCES = Path.of("resources/bogeys");
    private static final Path PROXY_CONFIG = Path.of("resources/common/models/model_proxy.json");

    @Test
    void everyBogeyObjectIsSelectedByTheProxyScanner() throws IOException {
        assertTrue(Files.isRegularFile(PROXY_CONFIG));

        List<String> patterns = new ArrayList<>();
        String config = Files.readString(PROXY_CONFIG);
        var namespace = Pattern.compile("\"kuayue\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL)
                .matcher(config);
        assertTrue(namespace.find());
        var patternMatcher = Pattern.compile("\"([^\"]+)\"").matcher(namespace.group(1));
        while (patternMatcher.find()) {
            patterns.add(patternMatcher.group(1));
        }
        assertFalse(patterns.isEmpty());

        List<String> unmatched;
        try (var paths = Files.walk(BOGEY_RESOURCES)) {
            unmatched = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".obj"))
                    .map(BogeyProxyConfigTest::bogeyModelPath)
                    .filter(path -> patterns.stream().noneMatch(glob -> matches(path, glob)))
                    .toList();
        }

        assertTrue(unmatched.isEmpty(), () -> "Bogey OBJ files are not selectable by "
                + PROXY_CONFIG + ":\n" + String.join("\n", unmatched));
    }

    private static String bogeyModelPath(Path path) {
        String normalized = path.toString().replace('\\', '/');
        int assetRoot = normalized.indexOf("/models/");
        if (assetRoot < 0) {
            throw new IllegalArgumentException("OBJ is outside models/: " + normalized);
        }
        return normalized.substring(assetRoot + 1);
    }

    private static boolean matches(String path, String pattern) {
        String regex = pattern
                .replace(".", "\\.")
                .replace("**", "\u00a7\u00a7")
                .replace("*", "[^/]*")
                .replace("\u00a7\u00a7", ".*")
                .replace("?", ".");
        return path.matches(regex);
    }
}
