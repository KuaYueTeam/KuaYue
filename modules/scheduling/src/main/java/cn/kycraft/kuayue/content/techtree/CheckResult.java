package cn.kycraft.kuayue.content.techtree;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Answer of the {@code tech_tree/can_unlock} pre-check.
 *
 * <p>Mirrors the reference {@code PlayerData.CheckReason}: the client needs to know what is still
 * missing (nodes, items, experience, visibility) to paint slots red/green and to fill the main
 * progress arrow.</p>
 */
public record CheckResult(boolean unlockable,
                          boolean seen,
                          boolean groupUnlocked,
                          boolean enoughLevel,
                          List<ItemStack> missingItems,
                          List<NodeLocation> missingNodes,
                          List<ItemStack> rewards,
                          float groupProgress) {

    private static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> ITEM_STACK_STREAM_CODEC =
            ItemStack.STREAM_CODEC.cast();

    public static final StreamCodec<FriendlyByteBuf, CheckResult> CODEC = StreamCodec.of(
            (buf, result) -> write(requireRegistry(buf), result),
            buf -> read(requireRegistry(buf)));

    private static RegistryFriendlyByteBuf requireRegistry(FriendlyByteBuf buf) {
        if (buf instanceof RegistryFriendlyByteBuf registry) return registry;
        throw new IllegalArgumentException("Tech tree check result requires a registry buffer");
    }

    private static void write(RegistryFriendlyByteBuf buf, CheckResult result) {
        buf.writeBoolean(result.unlockable);
        buf.writeBoolean(result.seen);
        buf.writeBoolean(result.groupUnlocked);
        buf.writeBoolean(result.enoughLevel);
        writeStacks(buf, result.missingItems);
        buf.writeVarInt(result.missingNodes.size());
        result.missingNodes.forEach(location -> location.write(buf));
        writeStacks(buf, result.rewards);
        buf.writeFloat(result.groupProgress);
    }

    private static CheckResult read(RegistryFriendlyByteBuf buf) {
        boolean unlockable = buf.readBoolean();
        boolean seen = buf.readBoolean();
        boolean groupUnlocked = buf.readBoolean();
        boolean enoughLevel = buf.readBoolean();
        List<ItemStack> missingItems = readStacks(buf);
        int nodeCount = buf.readVarInt();
        List<NodeLocation> missingNodes = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++) missingNodes.add(NodeLocation.read(buf));
        List<ItemStack> rewards = readStacks(buf);
        float progress = buf.readFloat();
        return new CheckResult(unlockable, seen, groupUnlocked, enoughLevel,
                missingItems, missingNodes, rewards, progress);
    }

    /** Vanilla ItemStack codec rejects EMPTY, so empty entries are dropped before encoding. */
    private static void writeStacks(RegistryFriendlyByteBuf buf, List<ItemStack> stacks) {
        List<ItemStack> present = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) present.add(stack);
        }
        buf.writeVarInt(present.size());
        present.forEach(stack -> ITEM_STACK_STREAM_CODEC.encode(buf, stack));
    }

    private static List<ItemStack> readStacks(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<ItemStack> stacks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) stacks.add(ITEM_STACK_STREAM_CODEC.decode(buf));
        return stacks;
    }

    public static CheckResult empty() {
        return new CheckResult(false, false, false, false, List.of(), List.of(), List.of(), 0f);
    }
}
