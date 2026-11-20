package cn.kycraft.kuayue.overhead_line.constants;

import cn.kycraft.kuayue.overhead_line.types.OverheadLineType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;

public class AllOverheadLineTypes {
    private static final HashMap<ResourceLocation, OverheadLineType> MAP =new HashMap<>();
    public static OverheadLineType register(ResourceLocation name, float maxVoltage, float maxCurrent, float maxLength){
        OverheadLineType type = new OverheadLineType(name,maxVoltage,maxCurrent,maxLength);
        MAP.put(name,type);
        return type;
    }

    public static boolean contains(ResourceLocation location){return MAP.containsKey(location);}

    public static OverheadLineType getType(ResourceLocation location){return MAP.getOrDefault(location,null);                                                                      }
}
