package cn.kycraft.kuayue.content.techtree;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaPositionType;

/**
 * Blueprint table as a LDLib2 block UI.
 *
 * <p>Going through {@link BlockUIMenuType} is what gives the table a working GUI: LDLib2 creates the
 * {@code ModularUIContainerMenu} and pairs it with a {@code ModularUIScreen}, so Escape and reopening behave
 * normally. A hand-rolled {@code AbstractContainerMenu} could never be displayed - vanilla had no screen to
 * build for it and closed it immediately.</p>
 *
 * <p>The slots are real {@link ItemSlot}s inside the UI, so click, shift-click and pickup are LDLib2's job;
 * the container only supplies the backing store.</p>
 */
public interface TechTreeMenuHost extends BlockUIMenuType.BlockUI {

    /** Board size in GUI pixels, matching the blueprint sheet. */
    int BOARD_W = 300;
    int BOARD_H = 150;
    /** Where the slot row sits inside the board. */
    int SLOT_X = 24;
    int SLOT_Y = 112;
    int SLOT_STEP = 22;

    @Override
    default ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        UIElement root = new UIElement();
        root.setId("tech_tree_board");
        root.layout(l -> l.setWidth(BOARD_W).setHeight(BOARD_H));

        // Backdrop as a real child: a background on the root element is drawn under the children, and the
        // board canvas covers the whole area, so the plate was invisible.
        UIElement paper = new UIElement();
        paper.setId("tech_tree_paper");
        paper.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, 0f).setPosition(YogaEdge.TOP, 0f)
                .setWidth(BOARD_W).setHeight(BOARD_H));
        paper.style(s -> s.background(TechTreeTextures.tableSheet()));
        root.addChild(paper);

        // Slots first: the board is added afterwards so node plates draw above the slot row instead of under it.
        TableSlots store = BlueprintTableRegistry.slotsFor(holder.pos);
        for (int i = 0; i < TableSlots.COUNT; i++) {
            ItemSlot slot = new ItemSlot();
            int index = i;
            slot.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                    .setPosition(YogaEdge.LEFT, (float) (SLOT_X + index * SLOT_STEP))
                    .setPosition(YogaEdge.TOP, (float) SLOT_Y)
                    .setWidth(18).setHeight(18));
            slot.bind(new net.minecraft.world.inventory.Slot(store, index, 0, 0));
            root.addChild(slot);
        }
        root.addChild(TechTreeBoard.buildGuiBoard());
        return ModularUI.of(UI.of(root), holder.player);
    }

    /** Title strip above the blue drawing field, inside the sheet's title area. */
    private static UIElement headerText() {
        Label label = new Label();
        label.setText(Component.literal("这是标题"));
        label.textStyle(s -> s.textColor(0xFF1E2A44).adaptiveHeight(true));
        label.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) (TechTreeBoard.FIELD_X + 4))
                .setPosition(YogaEdge.TOP, 0f)
                .setWidth(TechTreeBoard.FIELD_W - 8));
        return label;
    }

    /** Description inside the drawing field; the label wraps at the field width. */
    private static UIElement bodyText() {
        Label label = new Label();
        label.setText(Component.literal("这是介绍，介绍介绍介绍介绍介绍介绍介绍介绍"));
        label.textStyle(s -> s.textColor(0xFF1E2A44).adaptiveHeight(true));
        label.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) (TechTreeBoard.FIELD_X + 6))
                .setPosition(YogaEdge.TOP, (float) (TechTreeBoard.FIELD_Y + 4))
                .setWidth(TechTreeBoard.FIELD_W - 12));
        return label;
    }

    /** Clickable demo button below the drawing field. */
    private static UIElement demoButton(String caption, int index) {
        Button button = new Button();
        button.setText(Component.literal(caption));
        button.layout(l -> l.setPositionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.LEFT, (float) (TechTreeBoard.FIELD_X + 8 + index * 84))
                .setPosition(YogaEdge.TOP, (float) (TechTreeBoard.FIELD_Y + TechTreeBoard.FIELD_H + 6))
                .setWidth(76).setHeight(16));
        button.setOnClick(event -> {
            TechTreeSystem.LOGGER.info("Blueprint table button {} pressed", caption);
            event.stopPropagation();
        });
        return button;
    }

    @Override
    default boolean stillValid(BlockUIMenuType.BlockUIHolder holder) {
        BlockState state = holder.player.level().getBlockState(holder.pos);
        return state.getBlock() == holder.blockState.getBlock()
                && holder.player.distanceToSqr(holder.pos.getX() + 0.5d,
                holder.pos.getY() + 0.5d, holder.pos.getZ() + 0.5d) <= 64.0d;
    }

    @Override
    default Component getUIDisplayName(BlockUIMenuType.BlockUIHolder holder) {
        return Component.translatable("tech_tree.board.title");
    }

    /** Opens the table GUI; LDLib2 takes over both menu and screen. */
    static boolean openTable(ServerPlayer player, BlockPos pos) {
        return BlockUIMenuType.openUI(player, pos);
    }
}
