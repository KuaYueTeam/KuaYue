package cn.kycraft.kuayue.content.techtree;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** {@code namespace:group.node} identifier of a single node. */
public final class NodeLocation {

    private final String namespace;
    private final String group;
    private final String name;

    public NodeLocation(String namespace, String group, String name) {
        this.namespace = namespace;
        this.group = group;
        this.name = name;
    }

    /** Parses {@code group.node}, applying {@code defaultNamespace} / {@code defaultGroup} when omitted. */
    public static NodeLocation parse(String namespace, String group, String raw) {
        String cache = raw;
        String ns = namespace;
        String grp = group;
        int colon = cache.indexOf(':');
        if (colon >= 0) {
            ns = cache.substring(0, colon);
            cache = cache.substring(colon + 1);
        }
        int dot = cache.indexOf('.');
        if (dot >= 0) {
            grp = cache.substring(0, dot);
            cache = cache.substring(dot + 1);
        }
        return new NodeLocation(ns, grp, cache);
    }

    public static NodeLocation parseFull(String raw) {
        int colon = raw.indexOf(':');
        int dot = raw.indexOf('.', colon + 1);
        if (colon < 0 || dot < 0) {
            throw new IllegalArgumentException("Malformed node location: " + raw);
        }
        return new NodeLocation(raw.substring(0, colon), raw.substring(colon + 1, dot), raw.substring(dot + 1));
    }

    public String namespace() {
        return namespace;
    }

    public String group() {
        return group;
    }

    public String name() {
        return name;
    }

    public ResourceLocation groupId() {
        return ResourceLocation.fromNamespaceAndPath(namespace, group);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NodeLocation other
                && namespace.equals(other.namespace)
                && group.equals(other.group)
                && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return namespace + ":" + group + "." + name;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(toString());
    }

    public static NodeLocation read(FriendlyByteBuf buf) {
        return parseFull(buf.readUtf());
    }
}
