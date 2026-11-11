package cn.kycraft.kuayue.content.techtree;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
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
        root.style(s -> s.background(TechTreeTextures.boardPaper()));

        root.addChild(TechTreeBoard.buildGuiBoard());

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
        return ModularUI.of(UI.of(root), holder.player);
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
