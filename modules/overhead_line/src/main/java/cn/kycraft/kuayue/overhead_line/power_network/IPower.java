package cn.kycraft.kuayue.overhead_line.power_network;

import net.minecraft.nbt.CompoundTag;

public interface IPower {

    float getMaxVoltage();
    float getMaxCurrent();
    float getMaxPower();
    boolean isOverloaded(float voltage,float current);

    void write(CompoundTag compoundTag);
}
