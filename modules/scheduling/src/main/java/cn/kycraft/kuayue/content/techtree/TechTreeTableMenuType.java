package cn.kycraft.kuayue.content.techtree;

import lib.kasuga.registration.minecraft.menu.MenuReg;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;

/**
 * Menu type of {@link TechTreeTableMenu}.
 *
 * <p>Registered through Kasuga's {@link MenuReg}, not by hand: it posts the entry on the mod's
 * {@code REGISTER_EVENT} stage, which is the only point where {@code Registries.MENU} still accepts keys.
 * Registering lazily threw {@code Registry is already frozen} and took the whole module down at boot.</p>
 *
 * <p>Only the block position crosses the wire; both ends rebuild the block reference from it. No screen is
 * ever opened for this menu: the board is painted in the world.</p>
 */
final class TechTreeTableMenuType {

    /**
     * Declared as a field on purpose: Kasuga registers a {@code Reg} when the group it is parented to is
     * walked, so the instance has to exist before that pass rather than on first use.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static final MenuReg<TechTreeTableMenu, PlaceholderScreen> REG = (MenuReg) new MenuReg<>(
            "blueprint_table",
            (id, inventory, buffer) -> new TechTreeTableMenu(id, inventory, buffer.readBlockPos()),
            null)
            .setParent(TechTreeSystem.REGISTRY);

    private TechTreeTableMenuType() {
    }

    static MenuType<TechTreeTableMenu> get() {
        return REG.getEntry();
    }

    /**
     * Screen type parameter of {@link MenuReg}, never instantiated.
     *
     * <p>An abstract class rather than an interface because {@link MenuReg} requires
     * {@code U extends Screen & MenuAccess<T>}, and only a class can be a {@link Screen}.</p>
     */
    abstract static class PlaceholderScreen extends Screen implements MenuAccess<TechTreeTableMenu> {
        private PlaceholderScreen() {
            super(net.minecraft.network.chat.Component.empty());
        }
    }
}
