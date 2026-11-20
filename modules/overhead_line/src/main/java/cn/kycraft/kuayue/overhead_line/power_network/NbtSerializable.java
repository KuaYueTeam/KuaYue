package cn.kycraft.kuayue.overhead_line.power_network;

import net.minecraft.nbt.CompoundTag;

public interface NbtSerializable {
    void write(CompoundTag var1);
    void read(CompoundTag var2);
}
