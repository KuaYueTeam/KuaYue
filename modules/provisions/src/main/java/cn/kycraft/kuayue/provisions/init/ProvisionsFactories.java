package cn.kycraft.kuayue.provisions.init;

import cn.kycraft.kuayue.provisions.block.FoodType;
import cn.kycraft.kuayue.provisions.block.DriedInstantNoodlesBlock;
import cn.kycraft.kuayue.provisions.block.ParticlesDrinkBlock;
import cn.kycraft.kuayue.provisions.block.PlacementFoodBlock;
import cn.kycraft.kuayue.provisions.block.SoakedInstantNoodlesBlock;
import cn.kycraft.kuayue.provisions.block.TrainDietBoxBlock;
import cn.kycraft.kuayue.provisions.item.HandHeldFoodItem;
import cn.kycraft.kuayue.provisions.item.PlacementFoodBlockItem;
import com.google.gson.JsonObject;
import lib.kasuga.registration.Reg;
import lib.kasuga.registration.factory.FactoryRegistry;
import lib.kasuga.registration.minecraft.block.BlockReg;
import lib.kasuga.registration.minecraft.item.ItemReg;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * 本模块的 data-driven 工厂类型名，以及对应的 {@link FactoryRegistry} 注册。
 *
 * <p>Constants live here so {@code provisions.json} and the registration can never drift apart.
 * {@link #register()} is invoked from {@link cn.kycraft.kuayue.provisions.ProvisionsModule} while the
 * Micronaut context is built, which precedes the data-driven content pass that resolves types.</p>
 */
public final class ProvisionsFactories {

    public static final String SIMPLE_ITEM = "simple_item";
    public static final String DIET_BOX = "diet_box";
    public static final String DRINK = "drink";
    public static final String SOAKED_NOODLES = "soaked_noodles";
    public static final String PLACEMENT_FOOD = "placement_food";
    public static final String DRIED_NOODLES = "dried_noodles";

    private ProvisionsFactories() {
    }

    private static boolean registered;

    /** 幂等：重复调用只会注册一次。 */
    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;

        FactoryRegistry.registerItem(SIMPLE_ITEM, (id, params) -> {
            ItemReg<HandHeldFoodItem> reg = ItemReg.of(id,
                    (java.util.function.Function<net.minecraft.world.item.Item.Properties, HandHeldFoodItem>) HandHeldFoodItem::new);
            applyFood(reg, params);
            return (Reg<ItemReg<?>, Item>) (Reg<?, ?>) reg;
        });

        FactoryRegistry.register(DIET_BOX, (id, params) -> {
            BlockReg<TrainDietBoxBlock> reg = BlockReg.of(id,
                    (java.util.function.Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, TrainDietBoxBlock>)
                            p -> new TrainDietBoxBlock(p, FoodType.fromString(param(params, "food_type")),
                                    remainderId(params)));
            reg.withBlockItem(id, itemSupplier(params));
            return (Reg<BlockReg<?>, Block>) (Reg<?, ?>) reg;
        }, true);

        FactoryRegistry.register(DRINK, (id, params) -> {
            BlockReg<ParticlesDrinkBlock> reg = BlockReg.of(id,
                    (java.util.function.Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, ParticlesDrinkBlock>)
                            p -> new ParticlesDrinkBlock(p, FoodType.fromString(param(params, "food_type")),
                                    boolParam(params, "has_particle", true)));
            reg.withBlockItem(id, () -> (block, props) -> new cn.kycraft.kuayue.provisions.food.DrinkCanItem(
                    block, props, drinkFood(params), remainderItem(params)));
            return (Reg<BlockReg<?>, Block>) (Reg<?, ?>) reg;
        }, true);

        FactoryRegistry.register(PLACEMENT_FOOD, (id, params) -> {
            BlockReg<PlacementFoodBlock> reg = BlockReg.of(id,
                    (java.util.function.Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, PlacementFoodBlock>)
                            p -> new PlacementFoodBlock(p, FoodType.fromString(param(params, "food_type"))));
            reg.withBlockItem(id, itemSupplier(params));
            return (Reg<BlockReg<?>, Block>) (Reg<?, ?>) reg;
        }, true);

        FactoryRegistry.register(DRIED_NOODLES, (id, params) -> {
            BlockReg<DriedInstantNoodlesBlock> reg = BlockReg.of(id,
                    (java.util.function.Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, DriedInstantNoodlesBlock>)
                            p -> new DriedInstantNoodlesBlock(p, FoodType.fromString(param(params, "food_type"))));
            reg.withBlockItem(id, () -> (block, props) -> new cn.kycraft.kuayue.provisions.item.DriedInstantNoodlesBlockItem(
                    block, props, boolParam(params, "has_tooltip", true)));
            return (Reg<BlockReg<?>, Block>) (Reg<?, ?>) reg;
        }, true);

        FactoryRegistry.register(SOAKED_NOODLES, (id, params) -> {
            BlockReg<SoakedInstantNoodlesBlock> reg = BlockReg.of(id,
                    (java.util.function.Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, SoakedInstantNoodlesBlock>)
                            SoakedInstantNoodlesBlock::new);
            reg.withBlockItem(id, itemSupplier(params));
            return (Reg<BlockReg<?>, Block>) (Reg<?, ?>) reg;
        }, true);
    }

    private static Supplier<BiFunction<Block, Item.Properties, BlockItem>> itemSupplier(JsonObject params) {
        return () -> (block, props) -> new PlacementFoodBlockItem(block, props,
                boolParam(params, "has_tooltip", true));
    }

    private static String param(JsonObject params, String key) {
        return params != null && params.has(key) ? params.get(key).getAsString() : null;
    }

    /** 读取 params.remainder，即吃完后返还的物品 id。 */
    private static net.minecraft.resources.ResourceLocation remainderId(JsonObject params) {
        String raw = param(params, "remainder");
        return raw == null ? null : net.minecraft.resources.ResourceLocation.tryParse(raw);
    }

    /** 把 params.remainder 解析成延迟取物的 supplier：注册期间不能解引用。 */
    private static java.util.function.Supplier<? extends Item> remainderItem(JsonObject params) {
        net.minecraft.resources.ResourceLocation id = remainderId(params);
        return () -> id == null
                ? net.minecraft.world.item.Items.AIR
                : net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
    }

    /** 读取 params.food 对应的 DrinkCanItem 食物数值，缺省用可乐那份。 */
    private static net.minecraft.world.food.FoodProperties drinkFood(JsonObject params) {
        String kind = param(params, "drink");
        return "blue_bull".equals(kind)
                ? cn.kycraft.kuayue.provisions.food.DrinkRegistration.BLUE_BULL_FOOD
                : cn.kycraft.kuayue.provisions.food.DrinkRegistration.KUA_COLA_FOOD;
    }

    private static boolean boolParam(JsonObject params, String key, boolean fallback) {
        return params != null && params.has(key) ? params.get(key).getAsBoolean() : fallback;
    }

    /**
     * 把 params.food 应用到 {@link ItemReg} 的 food 修饰器上。
     *
     * <p>不走 {@code Item.Properties#food}：原版对声明了 crafting remainder 的物品
     * 会整段跳过食物处理，且 JsonItemParser 不认识 properties.food。</p>
     */
    private static void applyFood(ItemReg<?> reg, JsonObject params) {
        if (params == null || !params.has("food")) {
            return;
        }
        JsonObject f = params.getAsJsonObject("food");
        reg.isFood(true)
                .foodNutrition(f.has("nutrition") ? f.get("nutrition").getAsInt() : 1)
                .foodSaturationModifier(f.has("saturation") ? f.get("saturation").getAsFloat() : 0.5F);
        if (f.has("fast") && f.get("fast").getAsBoolean()) {
            reg.foodFast();
        }
        if (f.has("always_eat") && f.get("always_eat").getAsBoolean()) {
            reg.foodAlwaysEdible();
        }
    }
}
