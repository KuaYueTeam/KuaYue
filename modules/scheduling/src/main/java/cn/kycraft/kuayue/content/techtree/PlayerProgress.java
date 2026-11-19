package cn.kycraft.kuayue.content.techtree;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.attachment.AttachmentType;

import java.util.Objects;
import java.util.function.Supplier;

/** NeoForge data attachment holding one player's tech tree progress. */
public final class PlayerProgress {

    public static final ResourceLocation ATTACHMENT_ID = ResourceLocation.fromNamespaceAndPath("kuayue", "tech_tree");

    /**
     * Resolved lazily: the attachment registry does not exist while the mod object is being constructed,
     * so the holder must not be dereferenced during {@code @PostConstruct}.
     */
    static Supplier<AttachmentType<UnlockState>> SUPPLIER;

    /** @deprecated resolve through {@link #of(ServerPlayer)} so the supplier is honoured. */
    @Deprecated
    public static AttachmentType<UnlockState> TYPE;

    private PlayerProgress() {
    }

    public static AttachmentType<UnlockState> type() {
        AttachmentType<UnlockState> resolved = TYPE;
        if (resolved == null) {
            resolved = Objects.requireNonNull(SUPPLIER, "tech tree attachment was never registered").get();
        }
        return resolved;
    }

    public static UnlockState of(ServerPlayer player) {
        return player.getData(type());
    }
}
