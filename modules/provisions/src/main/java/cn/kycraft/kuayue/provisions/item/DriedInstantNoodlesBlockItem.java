package cn.kycraft.kuayue.provisions.item;

import cn.kycraft.kuayue.provisions.food.FoodProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 未冲泡的方便面：右键茶炉等 {@link FoodProcessor} 时变成泡好的面，否则正常放置。
 *
 * <p>1.0 用 {@code instanceof TeaBoilerBlock} 硬引用茶炉；这里改为查询接口，加工点自己决定
 * 能不能处理、产出什么、放什么音效和粒子。</p>
 */
public class DriedInstantNoodlesBlockItem extends PlacementFoodBlockItem {

    public DriedInstantNoodlesBlockItem(Block block, Properties properties, boolean hasTooltip) {
        super(block, properties, hasTooltip);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState clicked = level.getBlockState(pos);
        var player = context.getPlayer();
        if (player != null && clicked.getBlock() instanceof FoodProcessor processor) {
            var hand = context.getHand();
            ItemStack held = player.getItemInHand(hand);
            if (processor.canProcess(held)) {
                FoodProcessor.ProcessResult result = processor.process(level, pos, player, held);
                if (result != null) {
                    if (result.sound() != null) {
                        level.playSound(player, player.getX(), player.getY(), player.getZ(),
                                result.sound(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                    }
                    if (result.particle() != null && level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(result.particle(),
                                pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                                8, 0.1, 0.1, 0.1, 0.0);
                    }
                    player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, result.output(), false));
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }
        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("item.kuayue.tooltip.dried_instant_noodles.tip1"));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
