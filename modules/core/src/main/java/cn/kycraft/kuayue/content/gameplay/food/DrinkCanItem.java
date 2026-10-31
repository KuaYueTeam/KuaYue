package cn.kycraft.kuayue.content.gameplay.food;

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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

/**
 * 饮料罐: hold right-click to drink, then the empty can comes back.
 *
 * <p>Food values and effects are applied by hand in {@link #finishUsingItem} instead of through
 * {@code Item.Properties#food}. Vanilla's {@code Item} deliberately skips the food branch for
 * items that declare a crafting remainder, which silently swallowed the effects - doing it here
 * keeps the drink working and the empty can coming back.</p>
 */
public class DrinkCanItem extends Item {

    private final FoodProperties food;
    private final Supplier<? extends Item> container;

    public DrinkCanItem(Properties properties, FoodProperties food, Supplier<? extends Item> container) {
        super(properties);
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

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return super.finishUsingItem(stack, level, entity);
        }
        if (!level.isClientSide) {
            // nutrition / saturation
            player.getFoodData().eat(food.nutrition(), food.saturation());
            // alwaysEat effects
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
