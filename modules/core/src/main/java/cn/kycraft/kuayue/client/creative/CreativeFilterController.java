package cn.kycraft.kuayue.client.creative;

import cn.kycraft.kuayue.mixins.client.CreativeModeInventoryScreenAccessor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/** Per-screen baseline and selected vehicle filter for the vanilla creative categories. */
public final class CreativeFilterController {
    private static final Map<CreativeModeInventoryScreen, State> STATES = new WeakHashMap<>();
    private static volatile CreativeFilterCatalog catalog = new CreativeFilterCatalog(Map.of());
    private static volatile boolean tagsReady;

    private CreativeFilterController() {
    }

    public static void attach(
            CreativeModeInventoryScreen screen,
            Consumer<GuiEventListener> addListener
    ) {
        State state = state(screen);
        state.buttons.clear();
        ResourceLocation tab = tabId(screen);
        if (tab == null) return;
        List<CreativeFilterCatalog.Entry> entries = catalog.allEntries();
        if (entries.isEmpty()) return;
        int x = preferredX(screen);
        int y = screen.getGuiTop() + 18;
        if (entries.size() > 5 && screen.width - x < 84) {
            x = screen.getGuiLeft() - 86;
        }
        for (CreativeFilterCatalog.Entry entry : entries) {
            if (y + 20 > screen.height - 8) break;
            CreativeFilterButton button = new CreativeFilterButton(
                    x,
                    y,
                    entry,
                    entry.equals(state.selected),
                    () -> select(screen, entry)
            );
            state.buttons.add(button);
            addListener.accept(button);
            button.setVisible(entry.tab().equals(tab));
            y += 22;
        }
    }

    public static void refreshCatalog() {
        catalog = CreativeFilterCatalog.current();
    }

    public static void onTagsReady() {
        tagsReady = true;
        for (CreativeModeInventoryScreen screen : List.copyOf(STATES.keySet())) {
            State state = STATES.get(screen);
            if (state != null && state.selected != null) {
                refreshCurrent(screen);
            }
        }
    }

    public static void onTabChanged(CreativeModeInventoryScreen screen) {
        State state = state(screen);
        ResourceLocation tab = tabId(screen);
        if (!java.util.Objects.equals(tab, state.tab)) {
            state.tab = tab;
            state.selected = null;
        }
        state.buttons.forEach(button -> {
            button.setVisible(java.util.Objects.equals(tab, button.entryTab()));
            button.setSelected(button.matches(state.selected));
        });
        if (state.selected != null) {
            refreshCurrent(screen);
        }
    }

    public static void afterRefresh(CreativeModeInventoryScreen screen) {
        State state = STATES.get(screen);
        if (state == null || state.selected == null || !tagsReady) return;
        filter(screen, state.selected);
    }

    public static void detach(CreativeModeInventoryScreen screen) {
        STATES.remove(screen);
    }

    public static void disconnect() {
        STATES.clear();
        tagsReady = false;
        catalog = new CreativeFilterCatalog(Map.of());
    }

    private static void select(
            CreativeModeInventoryScreen screen,
            CreativeFilterCatalog.Entry entry
    ) {
        State state = state(screen);
        state.selected = entry.equals(state.selected) ? null : entry;
        state.buttons.forEach(button -> button.setSelected(entry.equals(state.selected)));
        refreshCurrent(screen);
    }

    private static void filter(
            CreativeModeInventoryScreen screen,
            CreativeFilterCatalog.Entry entry
    ) {
        List<ItemStack> filtered = new ArrayList<>();
        for (ItemStack stack : screen.getMenu().items) {
            if (stack.isEmpty()) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (entry.items().contains(id)
                    || entry.itemTags().stream().anyMatch(stack::is)) {
                filtered.add(stack.copy());
            }
        }
        screen.getMenu().items.clear();
        screen.getMenu().items.addAll(filtered);
        screen.getMenu().scrollTo(0.0F);
    }

    private static int preferredX(CreativeModeInventoryScreen screen) {
        int right = screen.getGuiLeft() + screen.getXSize() + 6;
        int preferred = right + 82 <= screen.width ? right : screen.getGuiLeft() - 88;
        return Math.max(4, Math.min(preferred, screen.width - 86));
    }

    private static void refreshCurrent(CreativeModeInventoryScreen screen) {
        CreativeModeTab tab = CreativeModeInventoryScreenAccessor.kuayue$selectedTab();
        ((CreativeModeInventoryScreenAccessor) screen)
                .kuayue$refreshCurrentTabContents(tab.getDisplayItems());
        State state = STATES.get(screen);
        if (state != null && state.selected != null && tagsReady) {
            filter(screen, state.selected);
        }
    }

    private static ResourceLocation tabId(CreativeModeInventoryScreen screen) {
        CreativeModeTab tab = CreativeModeInventoryScreenAccessor.kuayue$selectedTab();
        return BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tab)
                .map(ResourceKey::location)
                .orElse(null);
    }

    private static State state(CreativeModeInventoryScreen screen) {
        return STATES.computeIfAbsent(screen, ignored -> new State());
    }

    private static final class State {
        private ResourceLocation tab;
        private CreativeFilterCatalog.Entry selected;
        private final List<CreativeFilterButton> buttons = new ArrayList<>();
    }
}
