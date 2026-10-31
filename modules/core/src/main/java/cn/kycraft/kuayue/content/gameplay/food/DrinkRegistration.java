package cn.kycraft.kuayue.content.gameplay.food;

import cn.kycraft.kuayue.KuaYue;
import io.micronaut.context.annotation.Context;
import lib.kasuga.registration.RegistryGroup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 空罐 / 饮料罐 items.
 *
 * <p>Food values live on {@link DrinkCanItem}, not on {@code Item.Properties#food}, because vanilla
 * skips food handling for items that have a crafting remainder.</p>
 */
@Context
public class DrinkRegistration {

    public static final RegistryGroup GROUP_DRINK = new RegistryGroup()
            .setParent(KuaYue.REGISTRY);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, KuaYue.MODID);

    public static final FoodProperties KUA_COLA_FOOD = new FoodProperties.Builder()
            .nutrition(1)
            .saturationModifier(0.5F)
            .alwaysEdible()
            .effect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3600, 1), 1.0F)
            .build();

    public static final FoodProperties BLUE_BULL_FOOD = new FoodProperties.Builder()
            .nutrition(1)
            .saturationModifier(0.5F)
            .alwaysEdible()
            .effect(new MobEffectInstance(MobEffects.JUMP, 7200, 1), 1.0F)
            .build();

    public static final DeferredHolder<Item, EmptyCanItem> EMPTY_CAN =
            ITEMS.register("empty_can", () -> new EmptyCanItem(new Item.Properties().stacksTo(64)));

    public static final DeferredHolder<Item, EmptyCanItem> EMPTY_CAN_BLUE =
            ITEMS.register("empty_can_blue", () -> new EmptyCanItem(new Item.Properties().stacksTo(64)));

    public static final DeferredHolder<Item, DrinkCanItem> KUA_COLA =
            ITEMS.register("kua_cola", () -> new DrinkCanItem(
                    new Item.Properties().stacksTo(16),
                    KUA_COLA_FOOD,
                    () -> EMPTY_CAN.get()));

    public static final DeferredHolder<Item, DrinkCanItem> BLUE_BULL =
            ITEMS.register("blue_bull", () -> new DrinkCanItem(
                    new Item.Properties().stacksTo(16),
                    BLUE_BULL_FOOD,
                    () -> EMPTY_CAN_BLUE.get()));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
