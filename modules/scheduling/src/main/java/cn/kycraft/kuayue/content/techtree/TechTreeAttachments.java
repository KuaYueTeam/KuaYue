package cn.kycraft.kuayue.content.techtree;

import lib.kasuga.registration.Reg;
import lib.kasuga.registration.RegistryGroup;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Registers the per-player progress attachment. */
public final class TechTreeAttachments {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "kuayue");

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<UnlockState>> UNLOCK_STATE =
            ATTACHMENTS.register("tech_tree",
                    () -> AttachmentType.builder(UnlockState::new)
                            .serialize(new AttachmentSerializer())
                            .build());

    private static boolean initialized;

    private TechTreeAttachments() {
    }

    /**
     * Registers the attachment. Safe to call twice.
     *
     * <p>Deliberately does <b>not</b> resolve {@code UNLOCK_STATE.value()}: this runs from a Micronaut
     * {@code @PostConstruct}, i.e. while the mod object is still being constructed, which is far before
     * {@code neoforge:attachment_types} exists. Resolving it here aborts mod construction with
     * "Registry not present for DeferredHolder". The holder is resolved lazily on first player access.</p>
     */
    public static synchronized void init(IEventBus modEventBus) {
        if (initialized) return;
        initialized = true;
        ATTACHMENTS.register(modEventBus);
        PlayerProgress.SUPPLIER = () -> UNLOCK_STATE.value();
    }

    /** NBT (de)serialization of {@link UnlockState}. */
    private static final class AttachmentSerializer
            implements net.neoforged.neoforge.attachment.IAttachmentSerializer<
            net.minecraft.nbt.CompoundTag, UnlockState> {

        @Override
        public UnlockState read(net.neoforged.neoforge.attachment.IAttachmentHolder holder,
                                net.minecraft.nbt.CompoundTag tag,
                                net.minecraft.core.HolderLookup.Provider registries) {
            UnlockState state = new UnlockState();
            state.read(tag, registries);
            return state;
        }

        @Override
        public net.minecraft.nbt.CompoundTag write(UnlockState value,
                                                   net.minecraft.core.HolderLookup.Provider registries) {
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            value.write(tag, registries);
            return tag;
        }
    }
}
