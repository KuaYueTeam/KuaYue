package cn.kycraft.kuayue.packs;

import java.io.Serializable;
import java.util.Objects;

/**
 * A v1 dependency range: wildcard, exact version, or [lower, upper).
 */
public record VersionRange(String expression, Kind kind, SemanticVersion lower, SemanticVersion upper)
        implements Serializable {
    public enum Kind {
        WILDCARD,
        EXACT,
        HALF_OPEN
    }

    public VersionRange {
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(kind, "kind");
        if (kind == Kind.WILDCARD && !"*".equals(expression)) {
            throw new IllegalArgumentException("Wildcard range must be '*'");
        }
        if (kind == Kind.EXACT && (lower == null || upper != null)) {
            throw new IllegalArgumentException("Exact range requires only a lower version");
        }
        if (kind == Kind.HALF_OPEN && (lower == null || upper == null || lower.compareTo(upper) >= 0)) {
            throw new IllegalArgumentException("Half-open range requires lower < upper");
        }
    }

    public static VersionRange parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Version range is required");
        }
        String trimmed = value.trim();
        if (!trimmed.equals(value) || trimmed.isEmpty()) {
            throw new IllegalArgumentException("Version range must not have surrounding whitespace");
        }
        if ("*".equals(trimmed)) {
            return new VersionRange(trimmed, Kind.WILDCARD, null, null);
        }
        if (trimmed.startsWith("[") || trimmed.endsWith(")")) {
            if (!trimmed.startsWith("[") || !trimmed.endsWith(")")) {
                throw new IllegalArgumentException("Half-open range must use [lower,upper): " + value);
            }
            String body = trimmed.substring(1, trimmed.length() - 1);
            int comma = body.indexOf(',');
            if (comma < 1 || comma != body.lastIndexOf(',')) {
                throw new IllegalArgumentException("Half-open range requires two bounds: " + value);
            }
            String lowerText = body.substring(0, comma).trim();
            String upperText = body.substring(comma + 1).trim();
            SemanticVersion lower = SemanticVersion.parse(lowerText);
            SemanticVersion upper = SemanticVersion.parse(upperText);
            if (lower.compareTo(upper) >= 0) {
                throw new IllegalArgumentException("Range lower bound must be below upper bound: " + value);
            }
            return new VersionRange(trimmed, Kind.HALF_OPEN, lower, upper);
        }
        SemanticVersion exact = SemanticVersion.parse(trimmed);
        return new VersionRange(trimmed, Kind.EXACT, exact, null);
    }

    public boolean matches(SemanticVersion version) {
        Objects.requireNonNull(version, "version");
        return switch (kind) {
            case WILDCARD -> true;
            case EXACT -> lower.equals(version);
            case HALF_OPEN -> lower.compareTo(version) <= 0 && version.compareTo(upper) < 0;
        };
    }

    public boolean matches(String version, VersionComparator comparator) {
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(comparator, "comparator");
        if (kind == Kind.WILDCARD) {
            return true;
        }
        int lowerComparison = comparator.compare(version, lower.toString());
        if (kind == Kind.EXACT) {
            return lowerComparison == 0;
        }
        return lowerComparison >= 0 && comparator.compare(version, upper.toString()) < 0;
    }
}
