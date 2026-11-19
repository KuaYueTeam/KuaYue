package cn.kycraft.kuayue.content.techtree;

import net.minecraft.network.FriendlyByteBuf;

import java.util.Locale;

public enum NodeType {
    PLAIN,
    EPIC;

    public static NodeType byName(String name) {
        return "epic".equals(name) ? EPIC : PLAIN;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(name().toLowerCase(Locale.ROOT));
    }

    public static NodeType read(FriendlyByteBuf buf) {
        return byName(buf.readUtf());
    }
}
