package cn.kycraft.kuayue.packs;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Display translations frozen from one pack's own language files.
 */
public record PackDisplayText(Map<String, Translation> languages) implements Serializable {
    public PackDisplayText {
        Objects.requireNonNull(languages, "languages");
        LinkedHashMap<String, Translation> copy = new LinkedHashMap<>();
        languages.forEach((language, translation) -> copy.put(
                Objects.requireNonNull(language, "language"),
                Objects.requireNonNull(translation, "translation")
        ));
        languages = Collections.unmodifiableMap(copy);
    }

    public static PackDisplayText empty() {
        return new PackDisplayText(Map.of());
    }

    public String name(String language) {
        Translation translation = translation(language);
        return translation == null ? null : translation.name();
    }

    public String description(String language) {
        Translation translation = translation(language);
        return translation == null ? null : translation.description();
    }

    private Translation translation(String language) {
        Translation exact = languages.get(language);
        if (exact != null) return exact;
        return languages.get("en_us");
    }

    public record Translation(String name, String description) implements Serializable {
        public Translation {
            Objects.requireNonNull(name, "name");
        }
    }
}
