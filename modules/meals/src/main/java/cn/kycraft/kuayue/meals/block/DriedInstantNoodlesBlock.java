package cn.kycraft.kuayue.meals.block;

/**
 * 未冲泡的方便面方块：纯装饰/放置形态，转化逻辑在物品侧
 * ({@link cn.kycraft.kuayue.meals.item.DriedInstantNoodlesBlockItem})。
 */
public class DriedInstantNoodlesBlock extends PlacementFoodBlock {

    public DriedInstantNoodlesBlock(Properties properties, FoodType foodType) {
        super(properties, foodType);
    }
}
