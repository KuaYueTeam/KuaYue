package cn.kycraft.kuayue.client.settings;

import cn.kycraft.kuayue.packs.KuaYueSettingsService;
import cn.kycraft.kuayue.packs.PackIcon;
import cn.kycraft.kuayue.packs.PackDiagnostic;
import cn.kycraft.kuayue.packs.PackInstallationStatus;
import cn.kycraft.kuayue.packs.PackLimits;
import cn.kycraft.kuayue.packs.PackManifest;
import cn.kycraft.kuayue.packs.PackSelectionDraft;
import cn.kycraft.kuayue.packs.ParcelManifest;
import cn.kycraft.kuayue.packs.bootstrap.KuaYuePackBootstrap;
import cn.kycraft.kuayue.packs.bootstrap.StartupPackSnapshot;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.util.UISoundUtils;
import com.lowdragmc.lowdraglib2.math.Size;
import com.mojang.blaze3d.platform.NativeImage;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Concrete client menu pages. */
public final class KuaYueSettingsScreens {
    private static final ResourceLocation DEFAULT_FONT =
            ResourceLocation.withDefaultNamespace("default");
    private static final AtomicInteger NEXT_ICON_ID = new AtomicInteger();
    private static final IGuiTexture TRANSPARENT = new ColorRectTexture(0x00000000);
    private static final IGuiTexture PANEL_BACKGROUND = new ColorRectTexture(0x78000000);
    private static final IGuiTexture ROW_HOVER = new ColorRectTexture(0x30FFFFFF);
    private static final IGuiTexture RULE =
            new ColorRectTexture(0x556A6A6A);

    private KuaYueSettingsScreens() {
    }

    public static Screen mainScreen(Screen parent) {
        StartupPackSnapshot snapshot = KuaYuePackBootstrap.snapshot().orElse(null);
        if (snapshot == null) {
            return new MessageScreen(
                    Component.translatable("kuayue.settings.title"),
                    Component.translatable("kuayue.settings.unavailable"),
                    parent
            );
        }
        return new MainScreen(snapshot, parent);
    }

    private static ModularUI modularUI(UIElement root) {
        return ModularUI.of(UI.of(
                root,
                StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC)
        ));
    }

    private static ModularUI packModularUI(UIElement root) {
        return ModularUI.of(UI.of(
                root,
                List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC)),
                screen -> Size.of(
                        Math.max(1, screen.width - 8),
                        Math.max(1, screen.height - 8)
                )
        ));
    }

    private static Label label(String key) {
        return label(Component.translatable(key));
    }

    private static Label label(Component text) {
        Label label = new Label();
        label.setText(text);
        label.textStyle(style -> style.font(DEFAULT_FONT));
        return label;
    }

    private static Label styledLabel(
            Component text,
            float fontSize,
            int color,
            Horizontal alignment
    ) {
        Label label = label(text);
        label.textStyle(style -> style.font(DEFAULT_FONT)
                .fontSize(fontSize)
                .textColor(color)
                .textShadow(true)
                .textAlignHorizontal(alignment)
                .textAlignVertical(Vertical.CENTER));
        label.setAllowHitTest(false);
        return label;
    }

    private static Button button(String key) {
        return button(Component.translatable(key));
    }

    private static Button button(Component text) {
        Button button = new Button().setText(text);
        button.textStyle(style -> style.font(DEFAULT_FONT));
        return button;
    }

    private static Button textButton(String text) {
        Button button = new Button().setText(text, false);
        button.textStyle(style -> style.font(DEFAULT_FONT));
        return button;
    }

    private static final class MainScreen extends SettingsScreen {
        private MainScreen(StartupPackSnapshot snapshot, Screen parent) {
            super(
                    modularUI(mainRoot(snapshot, parent)),
                    Component.translatable("kuayue.settings.title"),
                    () -> Minecraft.getInstance().setScreen(parent)
            );
        }

        private static UIElement mainRoot(StartupPackSnapshot snapshot, Screen parent) {
            UIElement root = new UIElement()
                    .layout(layout -> layout.width(360).height(190).paddingAll(12).gapAll(10));
            root.addChildren(
                    label("kuayue.settings.title").layout(layout -> layout.height(18)),
                    new UIElement().layout(layout -> layout.flex(1))
            );
            UIElement row = new UIElement()
                    .layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(10).height(28));
            row.addChildren(
                    button("kuayue.settings.packs")
                            .setOnClick(event -> Minecraft.getInstance().setScreen(
                                    new PackScreen(snapshot, parent)
                            ))
                            .layout(layout -> layout.flex(1)),
                    button("kuayue.settings.done")
                            .setOnClick(event -> Minecraft.getInstance().setScreen(parent))
                            .layout(layout -> layout.flex(1))
            );
            root.addChild(row);
            return root;
        }
    }

    private static final class PackScreen extends SettingsScreen {
        private final StartupPackSnapshot snapshot;
        private final Screen parent;
        private final KuaYueSettingsService service;
        private KuaYueSettingsService.EditSession session;
        private PackSelectionDraft draft;
        private ScrollerView available;
        private ScrollerView selected;
        private Label error;
        private String initialError;
        private final List<PackInstallationStatus> unavailableInstallations;
        private final Map<String, ManagedIcon> packIcons = new LinkedHashMap<>();
        private final Set<String> failedPackIcons = new HashSet<>();

        private PackScreen(StartupPackSnapshot snapshot, Screen parent) {
            super(
                    packModularUI(new UIElement()),
                    Component.translatable("kuayue.settings.packs.title"),
                    () -> Minecraft.getInstance().setScreen(
                            MainScreenAccess.open(snapshot, parent))
            );
            this.snapshot = snapshot;
            this.parent = parent;
            this.service = new KuaYueSettingsService(snapshot.gameDirectory(), PackLimits.defaults());
            this.unavailableInstallations = KuaYueSettingsService.unavailableInstallations(
                    snapshot.knownManifests().keySet(),
                    snapshot.report()
            );
            KuaYueSettingsService.LoadResult loaded = service.begin(snapshot.knownManifests());
            this.session = loaded.session();
            this.initialError = loaded.diagnostics().stream()
                    .filter(cn.kycraft.kuayue.packs.PackDiagnostic::blocking)
                    .map(cn.kycraft.kuayue.packs.PackDiagnostic::message)
                    .collect(java.util.stream.Collectors.joining("\n"));
            if (session != null) this.draft = session.draft();
            buildContents();
        }

        private void buildContents() {
            UIElement root = modularUI.ui.rootElement;
            root.layout(layout -> layout.paddingAll(8).gapAll(6));
            root.clearAllChildren();

            UIElement heading = new UIElement()
                    .layout(layout -> layout.widthPercent(100).height(30).gapAll(1));
            heading.addChildren(
                    styledLabel(
                            Component.translatable("kuayue.settings.packs.title"),
                            9f,
                            0xFFFFFFFF,
                            Horizontal.CENTER
                    ).layout(layout -> layout.widthPercent(100).height(13)),
                    styledLabel(
                            Component.translatable("kuayue.settings.packs.pending"),
                            9f,
                            0xFFBDBDBD,
                            Horizontal.CENTER
                    ).textStyle(style -> style.textWrap(TextWrap.WRAP))
                            .layout(layout -> layout.widthPercent(100).height(13))
            );

            UIElement columns = new UIElement()
                    .layout(layout -> layout.flex(1).widthPercent(100)
                            .flexDirection(FlexDirection.ROW).gapAll(10));
            available = list();
            selected = list();
            columns.addChildren(column("kuayue.settings.available", available),
                    column("kuayue.settings.selected", selected));

            Label errorLabel = styledLabel(
                    Component.literal(initialError),
                    8f,
                    0xFFFF7070,
                    Horizontal.CENTER
            );
            errorLabel.layout(layout -> layout.widthPercent(100).height(13));
            error = errorLabel;
            error.setDisplay(!initialError.isBlank());

            UIElement footer = new UIElement()
                    .layout(layout -> layout.widthPercent(100).height(28)
                            .flexDirection(FlexDirection.ROW).gapAll(12)
                            .alignItems(AlignItems.CENTER)
                            .justifyContent(AlignContent.CENTER));
            Button openFolder = button("kuayue.settings.open_folder");
            Button apply = button("kuayue.settings.apply");
            Button cancel = button("kuayue.settings.cancel");
            openFolder.textStyle(style -> style.font(DEFAULT_FONT).fontSize(9f));
            apply.textStyle(style -> style.font(DEFAULT_FONT).fontSize(9f));
            cancel.textStyle(style -> style.font(DEFAULT_FONT).fontSize(9f));
            apply.setActive(session != null && draft != null);
            footer.addChildren(
                    openFolder.setOnClick(event -> openPacksFolder())
                            .layout(layout -> layout.flex(1).height(14)),
                    apply.setOnClick(event -> save())
                            .layout(layout -> layout.flex(1).height(14)),
                    cancel.setOnClick(event -> Minecraft.getInstance().setScreen(
                            MainScreenAccess.open(snapshot, parent)))
                            .layout(layout -> layout.flex(1).height(14))
            );
            root.addChildren(heading, columns, error, footer);
            rebuildLists();
        }

        private UIElement column(String title, ScrollerView list) {
            UIElement panel = new UIElement()
                    .style(style -> style.backgroundTexture(PANEL_BACKGROUND))
                    .layout(layout -> layout.flex(1).gapAll(4).paddingAll(8));
            UIElement header = new UIElement()
                    .layout(layout -> layout.widthPercent(100).height(21).gapAll(2));
            header.addChildren(
                    styledLabel(
                            Component.translatable(title),
                            9f,
                            0xFFFFFFFF,
                            Horizontal.CENTER
                    ).layout(layout -> layout.widthPercent(100).height(15)),
                    new UIElement()
                            .style(style -> style.backgroundTexture(RULE))
                            .layout(layout -> layout.widthPercent(100).height(1))
                            .setAllowHitTest(false)
            );
            panel.addChildren(header, list);
            return panel;
        }

        private ScrollerView list() {
            ScrollerView list = new ScrollerView()
                    .scrollerStyle(style -> style.mode(ScrollerMode.VERTICAL)
                            .adaptiveHeight(false)
                            .verticalScrollDisplay(ScrollDisplay.AUTO)
                            .horizontalScrollDisplay(ScrollDisplay.NEVER));
            list.layout(layout -> layout.flex(1));
            list.viewPort.style(style -> style.backgroundTexture(TRANSPARENT));
            list.viewContainer.layout(layout ->
                    layout.widthPercent(100).gapAll(2));
            return list;
        }

        private void rebuildLists() {
            if (available == null || selected == null || draft == null) return;
            modularUI.clearFocus();
            available.clearAllScrollViewChildren();
            selected.clearAllScrollViewChildren();
            for (PackInstallationStatus installation : unavailableInstallations) {
                available.addScrollViewChild(invalidInstallationRow(installation));
            }
            for (String id : draft.availableIds()) {
                available.addScrollViewChild(row(id, false));
            }
            for (String id : draft.selectedIdsByPriority()) {
                selected.addScrollViewChild(row(id, true));
            }
        }

        private UIElement invalidInstallationRow(PackInstallationStatus installation) {
            String packId = installation.packId() == null
                    ? installation.containerName()
                    : installation.packId();
            String source = Component.translatable(
                    "kuayue.settings.source."
                            + installation.sourceKind().name().toLowerCase()
            ).getString();
            String name = Component.translatable(
                    "kuayue.settings.invalid",
                    packId,
                    source
            ).getString();
            String codes = String.join(", ", installation.diagnosticCodes());
            PackDiagnostic diagnostic = snapshot.report().diagnostics().stream()
                    .filter(candidate -> installation.diagnosticCodes().contains(
                            candidate.code()))
                    .filter(candidate -> java.util.Objects.equals(
                                    candidate.sourceId(),
                                    installation.sourceId())
                            || java.util.Objects.equals(
                                    candidate.ownerId(),
                                    installation.packId()))
                    .findFirst()
                    .orElse(null);
            String detail = installation.sourceId()
                    + (codes.isBlank() ? "" : " · " + codes)
                    + (diagnostic == null ? "" : "\n" + diagnostic.message());
            Label nameLabel = styledLabel(
                    Component.literal(name),
                    9f,
                    0xFFFF8080,
                    Horizontal.LEFT
            );
            nameLabel.textStyle(style -> style.textWrap(TextWrap.HIDE));
            nameLabel.layout(layout -> layout.widthPercent(100).height(12));
            Label detailLabel = styledLabel(
                    Component.literal(detail),
                    8f,
                    0xFFFFB0B0,
                    Horizontal.LEFT
            );
            detailLabel.textStyle(style -> style.textWrap(TextWrap.WRAP).lineSpacing(1f));
            detailLabel.layout(layout -> layout.widthPercent(100).height(28));
            UIElement row = new UIElement()
                    .style(style -> style.backgroundTexture(new ColorRectTexture(0x35200000)))
                    .layout(layout -> layout.widthPercent(100).height(48).paddingAll(4)
                            .flexDirection(FlexDirection.COLUMN))
                    .setAllowHitTest(false);
            row.addChildren(nameLabel, detailLabel);
            return row;
        }

        private UIElement row(String id, boolean isSelected) {
            PackManifest manifest = session.knownManifests().get(id);
            String name = id;
            String description = "";
            var displayText = snapshot.packDisplayText(id);
            String language = Minecraft.getInstance().getLanguageManager().getSelected();
            if (displayText != null) {
                String localizedName = displayText.name(language);
                String localizedDescription = displayText.description(language);
                if (localizedName != null) {
                    name = localizedName;
                }
                if (localizedDescription != null) {
                    description = localizedDescription;
                }
            }
            if (manifest != null) {
                if (displayText == null || displayText.name(language) == null) {
                    String translated = Component.translatable(manifest.nameKey()).getString();
                    if (!manifest.nameKey().equals(translated)) {
                        name = translated;
                    }
                }
                if ((displayText == null || displayText.description(language) == null)
                        && manifest.descriptionKey() != null) {
                    String translatedDescription = Component.translatable(
                            manifest.descriptionKey()).getString();
                    if (!manifest.descriptionKey().equals(translatedDescription)) {
                        description = translatedDescription;
                    }
                }
            }

            String detail = description.isBlank()
                    ? id
                    : description + (description.contains(id) ? "" : " · " + id);
            Label nameLabel = styledLabel(
                    Component.literal(name),
                    10f,
                    0xFFFFFFFF,
                    Horizontal.LEFT
            );
            nameLabel.textStyle(style -> style.textWrap(TextWrap.HIDE));
            nameLabel.layout(layout -> layout.widthPercent(100).height(12));
            Label detailLabel = styledLabel(
                    Component.literal(detail),
                    8f,
                    0xFFB0B0B0,
                    Horizontal.LEFT
            );
            detailLabel.textStyle(style -> style.textWrap(TextWrap.WRAP).lineSpacing(1f));
            detailLabel.layout(layout -> layout.widthPercent(100).height(20));
            UIElement textBlock = new UIElement()
                    .layout(layout -> layout.flex(1).gapAll(1))
                    .setAllowHitTest(false);
            textBlock.addChildren(nameLabel, detailLabel);

            UIElement icon = new UIElement()
                    .style(style -> style.backgroundTexture(packIcon(id, manifest)))
                    .layout(layout -> layout.width(36).height(36))
                    .setAllowHitTest(false);
            UIElement row = new UIElement()
                    .style(style -> style.backgroundTexture(TRANSPARENT))
                    .layout(layout -> layout.widthPercent(100).height(46).paddingAll(4)
                            .flexDirection(FlexDirection.ROW).gapAll(8)
                            .alignItems(AlignItems.CENTER));
            row.addChildren(icon, textBlock);

            if (isSelected) {
                Button up = moveButton("▲", () -> draft.moveByPriority(id, -1));
                Button down = moveButton("▼", () -> draft.moveByPriority(id, 1));
                UIElement reorder = new UIElement()
                        .layout(layout -> layout.width(18).height(38).gapAll(2)
                                .flexDirection(FlexDirection.COLUMN)
                                .justifyContent(AlignContent.CENTER));
                reorder.addChildren(up, down);
                row.addChild(reorder);
            }

            row.addEventListener(UIEvents.MOUSE_ENTER, event ->
                    row.style(style -> style.backgroundTexture(ROW_HOVER)));
            row.addEventListener(UIEvents.MOUSE_LEAVE, event ->
                    row.style(style -> style.backgroundTexture(TRANSPARENT)));
            row.addEventListener(UIEvents.CLICK, event -> {
                if (event.button != 0) return;
                UISoundUtils.playButtonClickSound();
                if (isSelected) draft.remove(id); else draft.add(id);
                rebuildLists();
            });
            return row;
        }

        private Button moveButton(String text, Runnable move) {
            Button button = textButton(text);
            button.textStyle(style -> style.font(DEFAULT_FONT).fontSize(8f));
            button.buttonStyle(style -> style.baseTexture(TRANSPARENT)
                    .hoverTexture(ROW_HOVER)
                    .pressedTexture(new ColorRectTexture(0x55FFFFFF)));
            button.layout(layout -> layout.width(18).height(18));
            button.setOnClick(event -> {
                move.run();
                rebuildLists();
            });
            button.addEventListener(UIEvents.CLICK, event -> event.stopPropagation());
            return button;
        }

        private IGuiTexture packIcon(String packId, PackManifest manifest) {
            ManagedIcon cached = packIcons.get(packId);
            if (cached != null) {
                return cached.texture();
            }
            PackIcon icon = snapshot.packIcon(packId);
            if (icon == null || failedPackIcons.contains(packId)) {
                return new ItemStackTexture(fallbackItemIcon(manifest));
            }
            NativeImage image = null;
            try {
                image = NativeImage.read(new ByteArrayInputStream(icon.bytes()));
                ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                        "kuayue",
                        "dynamic/pack_icon/" + NEXT_ICON_ID.incrementAndGet()
                );
                Minecraft.getInstance().getTextureManager()
                        .register(location, new DynamicTexture(image));
                image = null;
                ManagedIcon managed = new ManagedIcon(SpriteTexture.of(location), location);
                packIcons.put(packId, managed);
                return managed.texture();
            } catch (IOException | RuntimeException failure) {
                failedPackIcons.add(packId);
            } finally {
                if (image != null) {
                    image.close();
                }
            }
            return new ItemStackTexture(fallbackItemIcon(manifest));
        }

        private ItemStack fallbackItemIcon(PackManifest manifest) {
            if (manifest == null) return new ItemStack(Items.MINECART);
            StartupPackSnapshot.ManagedPackRoot root = snapshot.resourceRoot(manifest.id());
            if (root == null) return new ItemStack(Items.MINECART);
            List<ParcelManifest> parcels = root.parcels().values().stream()
                    .sorted(java.util.Comparator.comparing(ParcelManifest::id))
                    .toList();
            for (ParcelManifest parcel : parcels) {
                if (parcel.creativeFilter() == null
                        || parcel.creativeFilter().iconItem() == null) continue;
                var id = net.minecraft.resources.ResourceLocation.tryParse(
                        parcel.creativeFilter().iconItem());
                if (id == null) continue;
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
                if (item != Items.AIR) return new ItemStack(item);
            }
            return new ItemStack(Items.MINECART);
        }

        private void save() {
            if (session == null || draft == null) return;
            KuaYueSettingsService.SaveResult result = service.save(session, draft);
            if (result.successful()) {
                Minecraft.getInstance().setScreen(new NoticeScreen(snapshot, parent));
                return;
            }
            error.setText(result.diagnostic().message(), false);
            error.setDisplay(true);
        }

        private void openPacksFolder() {
            Path folder = snapshot.gameDirectory().resolve("kuayue").resolve("packs");
            try {
                Files.createDirectories(folder);
                Util.getPlatform().openFile(folder.toFile());
            } catch (IOException | RuntimeException failure) {
                error.setText(
                        Component.translatable("kuayue.settings.open_folder.failed").getString(),
                        false
                );
                error.setDisplay(true);
            }
        }

        @Override
        public void removed() {
            try {
                super.removed();
            } finally {
                for (ManagedIcon icon : packIcons.values()) {
                    Minecraft.getInstance().getTextureManager().release(icon.location());
                }
                packIcons.clear();
                failedPackIcons.clear();
            }
        }
    }

    private record ManagedIcon(IGuiTexture texture, ResourceLocation location) {
    }

    private static final class NoticeScreen extends SettingsScreen {
        private NoticeScreen(StartupPackSnapshot snapshot, Screen parent) {
            super(
                    modularUI(noticeRoot(snapshot, parent)),
                    Component.translatable("kuayue.settings.notice.title"),
                    () -> Minecraft.getInstance().setScreen(MainScreenAccess.open(snapshot, parent))
            );
        }

        private static UIElement noticeRoot(StartupPackSnapshot snapshot, Screen parent) {
            UIElement root = new UIElement()
                    .layout(layout -> layout.width(320).height(140).paddingAll(12).gapAll(10));
            root.addChildren(
                    label("kuayue.settings.notice.title"),
                    label("kuayue.settings.notice.body"),
                    new UIElement().layout(layout -> layout.flex(1)),
                    button("kuayue.settings.notice.ok")
                            .setOnClick(event -> Minecraft.getInstance().setScreen(
                                    MainScreenAccess.open(snapshot, parent)))
                            .layout(layout -> layout.height(22))
            );
            return root;
        }
    }

    private static final class MessageScreen extends SettingsScreen {
        private MessageScreen(Component title, Component message, Screen parent) {
            super(
                    modularUI(messageRoot(message, parent)),
                    title,
                    () -> Minecraft.getInstance().setScreen(parent)
            );
        }

        private static UIElement messageRoot(Component message, Screen parent) {
            UIElement root = new UIElement()
                    .layout(layout -> layout.width(320).height(120).paddingAll(12).gapAll(8));
            Label label = label(message);
            root.addChildren(
                    label,
                    new UIElement().layout(layout -> layout.flex(1)),
                    button(Component.translatable("gui.done"))
                            .setOnClick(event -> Minecraft.getInstance().setScreen(parent))
            );
            return root;
        }
    }

    private static final class MainScreenAccess {
        private static Screen open(StartupPackSnapshot snapshot, Screen parent) {
            return new MainScreen(snapshot, parent);
        }
    }
}
