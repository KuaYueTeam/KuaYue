package cn.kycraft.kuayue.meals.item;

import cn.kycraft.kuayue.content.gameplay.food.effect.EffectUtil;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;

/**
 * 放置类食物的物品形态：只能放在方块顶面。
 *
 * <p>1.0 的 {@code canPlace} 直接拒绝非 UP 面，这里保留同样行为。</p>
 */
public class PlacementFoodBlockItem extends BlockItem {

    private final boolean hasTooltip;

    public PlacementFoodBlockItem(Block block, Properties properties, boolean hasTooltip) {
        super(block, properties);
        this.hasTooltip = hasTooltip;
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        if (context.getClickedFace() != Direction.UP) {
            return false;
        }
        return super.canPlace(context, state);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        if (hasTooltip) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            tooltip.add(Component.translatable("item.kuayue.tooltip." + id.getPath() + ".tip0"));
        }
        EffectUtil.addFoodEffectToTooltip(stack, context, tooltip, flag);
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
