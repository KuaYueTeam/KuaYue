package cn.kycraft.kuayue.packs;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The strict three-component version subset used by pack format v1.
 */
public record SemanticVersion(BigInteger major, BigInteger minor, BigInteger patch)
        implements Comparable<SemanticVersion>, Serializable {
    private static final Pattern PATTERN =
            Pattern.compile("^(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)$");
    private static final int MAX_VERSION_LENGTH = 1_024;

    public SemanticVersion {
        Objects.requireNonNull(major, "major");
        Objects.requireNonNull(minor, "minor");
        Objects.requireNonNull(patch, "patch");
        if (major.signum() < 0 || minor.signum() < 0 || patch.signum() < 0) {
            throw new IllegalArgumentException("Version components must be non-negative");
        }
    }

    public static SemanticVersion parse(String value) {
        if (value == null || value.length() > MAX_VERSION_LENGTH) {
            throw new IllegalArgumentException("Invalid semantic version: " + value);
        }
        Matcher matcher = PATTERN.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Expected a strict major.minor.patch version: " + value);
        }
        try {
            return new SemanticVersion(
                    new BigInteger(matcher.group(1)),
                    new BigInteger(matcher.group(2)),
                    new BigInteger(matcher.group(3)));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid semantic version: " + value, exception);
        }
    }

    public static int compareText(String left, String right) {
        return parse(left).compareTo(parse(right));
    }

    @Override
    public int compareTo(SemanticVersion other) {
        int result = major.compareTo(other.major);
        if (result != 0) {
            return result;
        }
        result = minor.compareTo(other.minor);
        if (result != 0) {
            return result;
        }
        return patch.compareTo(other.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
