package cn.kycraft.kuayue.packs;

import java.util.regex.Pattern;

public final class PackPathRules {
    private static final Pattern PACK_ID = Pattern.compile("[a-z][a-z0-9_-]*");
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern RESOURCE_PATH =
            Pattern.compile("[a-z0-9_.-]+(?:/[a-z0-9_.-]+)*");
    private static final Pattern PARCEL_FOLDER = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern IMAGE_EXTENSION =
            Pattern.compile(".+\\.(?:png|jpg|jpeg|webp)", Pattern.CASE_INSENSITIVE);

    private PackPathRules() {
    }

    public static boolean isPackId(String value) {
        return value != null && PACK_ID.matcher(value).matches();
    }

    public static boolean isNamespace(String value) {
        return value != null && NAMESPACE.matcher(value).matches();
    }

    public static boolean isResourcePath(String value) {
        return value != null && RESOURCE_PATH.matcher(value).matches();
    }

    public static boolean isResourceLocation(String value) {
        if (value == null) {
            return false;
        }
        int colon = value.indexOf(':');
        return colon > 0
                && colon == value.lastIndexOf(':')
                && isNamespace(value.substring(0, colon))
                && isResourcePath(value.substring(colon + 1));
    }

    public static boolean isSafeRelativePath(String value) {
        if (value == null || value.isEmpty() || value.startsWith("/")
                || value.indexOf('\\') >= 0 || value.indexOf('\0') >= 0
                || value.indexOf(':') >= 0) {
            return false;
        }
        String[] segments = value.split("/", -1);
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isParcelManifestPath(String value) {
        if (!isSafeRelativePath(value)) {
            return false;
        }
        String[] segments = value.split("/", -1);
        return segments.length == 3
                && "parcels".equals(segments[0])
                && PARCEL_FOLDER.matcher(segments[1]).matches()
                && "parcel.json".equals(segments[2]);
    }

    public static boolean isKasugaIndexPath(String value, String namespace) {
        return isSafeRelativePath(value)
                && value.startsWith("data/" + namespace + "/kasuga_lib/data_driven/")
                && value.endsWith(".json");
    }

    public static boolean isIconPath(String value) {
        return isResourceLocation(value)
                && value.substring(value.indexOf(':') + 1).startsWith("textures/")
                && IMAGE_EXTENSION.matcher(value).matches();
    }

    public static String namespaceOf(String resourceLocation) {
        int colon = resourceLocation.indexOf(':');
        return colon < 0 ? null : resourceLocation.substring(0, colon);
    }

    public static String pathOf(String resourceLocation) {
        int colon = resourceLocation.indexOf(':');
        return colon < 0 ? null : resourceLocation.substring(colon + 1);
    }
}
