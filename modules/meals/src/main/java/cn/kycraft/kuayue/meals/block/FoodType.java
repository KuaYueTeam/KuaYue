package cn.kycraft.kuayue.meals.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 食物方块的外形分类，决定碰撞箱。 */
public enum FoodType {
    BOX(Block.box(1, 0, 1, 15, 2, 15)),
    BOWL(Block.box(3, 0, 3, 13, 9, 13)),
    BOTTLE(Block.box(5, 0, 5, 11, 7, 11));

    private final VoxelShape shape;

    FoodType(VoxelShape shape) {
        this.shape = shape;
    }

    public VoxelShape shape() {
        return shape;
    }

    /** 从 JSON 的 params.food_type 解析，缺省为 BOX。 */
    public static FoodType fromString(String name) {
        if (name == null) {
            return BOX;
        }
        for (FoodType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return BOX;
    }
}
