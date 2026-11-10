package cn.kycraft.kuayue.provisions.food;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * 饮料罐：手持时是饮品，右键方块时是放置物。
 *
 * <p>继承 {@link BlockItem} 让它能放下 {@code drink} 工厂生成的方块，同时重写 {@link #use}
 * 提供饮用行为——原版 {@code BlockItem#use} 只处理放置，不会开始使用物品。</p>
 *
 * <p>食物数值与效果在 {@link #finishUsingItem} 里手动结算，不走
 * {@code Item.Properties#food}：原版对声明了 crafting remainder 的物品会整段跳过食物处理。</p>
 *
 * <p>方块通过 {@link Supplier} 传入。物品注册与方块注册是两个独立的 {@code RegisterEvent}
 * 阶段，构造时解引用会撞上未绑定的注册项。</p>
 */
public class DrinkCanItem extends BlockItem {

    private final FoodProperties food;
    private final Supplier<? extends Item> container;

    public DrinkCanItem(Block block, Properties properties, FoodProperties food,
                        Supplier<? extends Item> container) {
        super(block, properties);
        this.food = food;
        this.container = container;
    }

    public FoodProperties food() {
        return food;
    }

    public Item container() {
        return container.get();
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return new ItemStack(container());
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    /**
     * 潜行时交给 {@link BlockItem} 的放置逻辑，否则开始饮用。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return super.finishUsingItem(stack, level, entity);
        }
        if (!level.isClientSide) {
            player.getFoodData().eat(food.nutrition(), food.saturation());
            for (FoodProperties.PossibleEffect possible : food.effects()) {
                if (level.getRandom().nextFloat() < possible.probability()) {
                    player.addEffect(new MobEffectInstance(possible.effect()));
                }
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_DRINK,
                    SoundSource.PLAYERS, 0.5F,
                    0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
            player.awardStat(Stats.ITEM_USED.get(this));
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
            }
        }
        return ItemUtils.createFilledResult(stack, player, new ItemStack(container()));
    }
}
