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
import java.util.HashMap;
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
        state.buttonsByTab.clear();
        state.offsetsByTab.clear();

        ResourceLocation tab = tabId(screen);
        if (tab == null) return;
        state.tab = tab;

        for (CreativeFilterCatalog.Entry entry : catalog.allEntries()) {
            CreativeFilterButton button = new CreativeFilterButton(
                    0,
                    0,
                    entry,
                    () -> select(screen, entry)
            );
            state.buttons.add(button);
            state.buttonsByTab
                    .computeIfAbsent(entry.tab(), ignored -> new ArrayList<>())
                    .add(button);
            addListener.accept(button);
        }

        state.pageUp = new CreativeFilterScrollButton(
                0,
                0,
                CreativeFilterScrollButton.Direction.UP,
                () -> scroll(screen, -1)
        );
        state.pageDown = new CreativeFilterScrollButton(
                0,
                0,
                CreativeFilterScrollButton.Direction.DOWN,
                () -> scroll(screen, 1)
        );
        addListener.accept(state.pageUp);
        addListener.accept(state.pageDown);
        layout(screen, state, tab);
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
        layout(screen, state, tab);
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
        layout(screen, state, state.tab);
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

    private static void scroll(CreativeModeInventoryScreen screen, int direction) {
        State state = STATES.get(screen);
        if (state == null || state.tab == null) return;
        List<CreativeFilterButton> buttons =
                state.buttonsByTab.getOrDefault(state.tab, List.of());
        int maximum = Math.max(0, buttons.size() - 5);
        int offset = state.offsetsByTab.getOrDefault(state.tab, 0);
        state.offsetsByTab.put(
                state.tab,
                Math.max(0, Math.min(maximum, offset + direction))
        );
        layout(screen, state, state.tab);
    }

    private static void layout(
            CreativeModeInventoryScreen screen,
            State state,
            ResourceLocation tab
    ) {
        state.buttons.forEach(button -> button.setVisible(false));
        if (tab == null || state.pageUp == null || state.pageDown == null) {
            hidePageButtons(state);
            return;
        }

        List<CreativeFilterButton> buttons =
                state.buttonsByTab.getOrDefault(tab, List.of());
        int maximum = Math.max(0, buttons.size() - 5);
        int offset = Math.max(
                0,
                Math.min(maximum, state.offsetsByTab.getOrDefault(tab, 0))
        );
        state.offsetsByTab.put(tab, offset);

        int collapsedX = screen.getGuiLeft() - CreativeFilterButton.SIZE - 2;
        int firstY = screen.getGuiTop() + 14;
        for (int index = 0; index < buttons.size(); index++) {
            CreativeFilterButton button = buttons.get(index);
            boolean selected = button.matches(state.selected);
            button.setSelected(selected);
            button.setX(collapsedX - (selected ? CreativeFilterButton.SIZE : 0));
            button.setY(firstY + (index - offset) * 22);
            button.setVisible(index >= offset && index < offset + 5);
        }

        boolean overflowing = buttons.size() > 5;
        state.pageUp.visible = overflowing;
        state.pageDown.visible = overflowing;
        state.pageUp.active = overflowing && offset > 0;
        state.pageDown.active = overflowing && offset < maximum;
        state.pageUp.setX(collapsedX);
        state.pageDown.setX(collapsedX);
        state.pageUp.setY(screen.getGuiTop() - 8);
        state.pageDown.setY(firstY + 5 * 22);
    }

    private static void hidePageButtons(State state) {
        if (state.pageUp != null) state.pageUp.visible = false;
        if (state.pageDown != null) state.pageDown.visible = false;
    }

    private static State state(CreativeModeInventoryScreen screen) {
        return STATES.computeIfAbsent(screen, ignored -> new State());
    }

    private static final class State {
        private ResourceLocation tab;
        private CreativeFilterCatalog.Entry selected;
        private final List<CreativeFilterButton> buttons = new ArrayList<>();
        private final Map<ResourceLocation, List<CreativeFilterButton>> buttonsByTab =
                new HashMap<>();
        private final Map<ResourceLocation, Integer> offsetsByTab = new HashMap<>();
        private CreativeFilterScrollButton pageUp;
        private CreativeFilterScrollButton pageDown;
    }
}
