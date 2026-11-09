package cn.kycraft.kuayue.provisions.block;

import net.minecraft.util.StringRepresentable;

/** 泡面三状态机（与 1.0 instant_noodles 的 noodles_state 属性一致）。 */
public enum NoodlesState implements StringRepresentable {
    WITH_FORK("with_fork"),
    OPENING("opening"),
    FINISH_EATING("finish_eating");

    public static final com.mojang.serialization.Codec<NoodlesState> CODEC =
            StringRepresentable.fromEnum(NoodlesState::values);
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<NoodlesState> PROPERTY =
            net.minecraft.world.level.block.state.properties.EnumProperty.create("noodles_state", NoodlesState.class);

    private final String name;

    NoodlesState(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
