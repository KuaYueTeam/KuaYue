package cn.kycraft.kuayue.content.techtree;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/**
 * Menu type of {@link TechTreeTableMenu}.
 *
 * <p>Not built through {@code BlockUIMenuType}: that helper always pairs the menu with a
 * {@code ModularUIScreen}, and this table deliberately has none. Both ends rebuild the
 * {@link BlockUIMenuType.BlockUIHolder} from the block position, which is the only thing written to the
 * client.</p>
 */
final class TechTreeTableMenuType {

    private static MenuType<ModularUIContainerMenu> entry;

    private TechTreeTableMenuType() {
    }

    @SuppressWarnings("unchecked")
    static synchronized MenuType<ModularUIContainerMenu> get() {
        if (entry == null) {
            entry = (MenuType<ModularUIContainerMenu>) (MenuType<?>) createEntry();
            Registry.register(BuiltInRegistries.MENU,
                    ResourceLocation.fromNamespaceAndPath("kuayue", "blueprint_table"), entry);
        }
        return entry;
    }

    private static MenuType<?> createEntry() {
        return new MenuType<TechTreeTableMenu>(
                new net.minecraft.world.inventory.MenuType.MenuSupplier<TechTreeTableMenu>() {
                    @Override
                    public TechTreeTableMenu create(int id, net.minecraft.world.entity.player.Inventory inventory) {
                        // The supplier form has no buffer; the position arrives through the open-data
                        // variant below, so this path is only a fallback for the base ctor.
                        return new TechTreeTableMenu(id, inventory,
                                holder(inventory.player, inventory.player.blockPosition()));
                    }
                }, FeatureFlags.DEFAULT_FLAGS);
    }

    /** Holder rebuilt from the block itself; {@code BlueprintTableBlock} is the {@code BlockUI}. */
    static BlockUIMenuType.BlockUIHolder holder(net.minecraft.world.entity.player.Player player, BlockPos pos) {
        var state = player.level().getBlockState(pos);
        var block = state.getBlock();
        BlockUIMenuType.BlockUI ui = block instanceof BlockUIMenuType.BlockUI found ? found : null;
        return new BlockUIMenuType.BlockUIHolder(ui, player, pos, state);
    }
}
