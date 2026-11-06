package cn.kycraft.kuayue.meals.init;

import cn.kycraft.kuayue.meals.block.FoodType;
import cn.kycraft.kuayue.meals.block.DriedInstantNoodlesBlock;
import cn.kycraft.kuayue.meals.block.ParticlesDrinkBlock;
import cn.kycraft.kuayue.meals.block.PlacementFoodBlock;
import cn.kycraft.kuayue.meals.block.SoakedInstantNoodlesBlock;
import cn.kycraft.kuayue.meals.block.TrainDietBoxBlock;
import cn.kycraft.kuayue.meals.item.HandHeldFoodItem;
import cn.kycraft.kuayue.meals.item.PlacementFoodBlockItem;
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
 * {@link #register()} is invoked from {@link cn.kycraft.kuayue.meals.MealsModule} while the
 * Micronaut context is built, which precedes the data-driven content pass that resolves types.</p>
 */
public final class MealsFactories {

    public static final String SIMPLE_ITEM = "simple_item";
    public static final String DIET_BOX = "diet_box";
    public static final String DRINK = "drink";
    public static final String SOAKED_NOODLES = "soaked_noodles";
    public static final String PLACEMENT_FOOD = "placement_food";
    public static final String DRIED_NOODLES = "dried_noodles";

    private MealsFactories() {
    }

    private static boolean registered;

    /** 幂等：重复调用只会注册一次。 */
    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;

        FactoryRegistry.registerItem(SIMPLE_ITEM, (id, params) -> ItemReg.of(id, props -> new HandHeldFoodItem(food(props, params))));

        FactoryRegistry.register(DIET_BOX, (id, params) -> {
            BlockReg<TrainDietBoxBlock> reg = BlockReg.of(id,
                    (java.util.function.Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, TrainDietBoxBlock>)
                            p -> new TrainDietBoxBlock(p, FoodType.fromString(param(params, "food_type"))));
            reg.withBlockItem(id, itemSupplier(params));
            return (Reg<BlockReg<?>, Block>) (Reg<?, ?>) reg;
        });

        FactoryRegistry.register(DRINK, (id, params) -> {
            BlockReg<ParticlesDrinkBlock> reg = BlockReg.of(id,
                    (java.util.function.Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, ParticlesDrinkBlock>)
                            p -> new ParticlesDrinkBlock(p, FoodType.fromString(param(params, "food_type")),
                                    boolParam(params, "has_particle", true)));
            reg.withBlockItem(id, itemSupplier(params));
            return (Reg<BlockReg<?>, Block>) (Reg<?, ?>) reg;
        });

        FactoryRegistry.register(PLACEMENT_FOOD, (id, params) -> {
            BlockReg<PlacementFoodBlock> reg = BlockReg.of(id,
                    (java.util.function.Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, PlacementFoodBlock>)
                            p -> new PlacementFoodBlock(p, FoodType.fromString(param(params, "food_type"))));
            reg.withBlockItem(id, itemSupplier(params));
            return (Reg<BlockReg<?>, Block>) (Reg<?, ?>) reg;
        });

        FactoryRegistry.register(DRIED_NOODLES, (id, params) -> {
            BlockReg<DriedInstantNoodlesBlock> reg = BlockReg.of(id,
                    (java.util.function.Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, DriedInstantNoodlesBlock>)
                            p -> new DriedInstantNoodlesBlock(p, FoodType.fromString(param(params, "food_type"))));
            reg.withBlockItem(id, () -> (block, props) -> new cn.kycraft.kuayue.meals.item.DriedInstantNoodlesBlockItem(
                    block, props, boolParam(params, "has_tooltip", true)));
            return (Reg<BlockReg<?>, Block>) (Reg<?, ?>) reg;
        });

        FactoryRegistry.register(SOAKED_NOODLES, (id, params) -> {
            BlockReg<SoakedInstantNoodlesBlock> reg = BlockReg.of(id,
                    (java.util.function.Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, SoakedInstantNoodlesBlock>)
                            SoakedInstantNoodlesBlock::new);
            reg.withBlockItem(id, itemSupplier(params));
            return (Reg<BlockReg<?>, Block>) (Reg<?, ?>) reg;
        });
    }

    private static Supplier<BiFunction<Block, Item.Properties, BlockItem>> itemSupplier(JsonObject params) {
        return () -> (block, props) -> new PlacementFoodBlockItem(block, props,
                boolParam(params, "has_tooltip", true));
    }

    private static String param(JsonObject params, String key) {
        return params != null && params.has(key) ? params.get(key).getAsString() : null;
    }

    private static boolean boolParam(JsonObject params, String key, boolean fallback) {
        return params != null && params.has(key) ? params.get(key).getAsBoolean() : fallback;
    }

    /**
     * 从 params 读取食物数值（properties 里的 food 会被 JsonItemParser 丢弃）。
     * 没有 food 段时返回空属性，得到普通物品。
     */
    private static Item.Properties food(Item.Properties props, JsonObject params) {
        if (params == null || !params.has("food")) {
            return props;
        }
        JsonObject f = params.getAsJsonObject("food");
        FoodProperties.Builder builder = new FoodProperties.Builder()
                .nutrition(f.has("nutrition") ? f.get("nutrition").getAsInt() : 1)
                .saturationModifier(f.has("saturation") ? f.get("saturation").getAsFloat() : 0.5F);
        if (f.has("fast") && f.get("fast").getAsBoolean()) {
            builder.fast();
        }
        if (f.has("always_eat") && f.get("always_eat").getAsBoolean()) {
            builder.alwaysEdible();
        }
        return props.food(builder.build());
    }
}
