package cn.kycraft.kuayue.overhead_line.power_network;

import cn.kycraft.kuayue.overhead_line.constants.AllPowerNodeTypes;
import cn.kycraft.kuayue.overhead_line.constants.Utils;
import cn.kycraft.kuayue.overhead_line.types.OverheadLineType;
import cn.kycraft.kuayue.overhead_line.types.PowerNodeType;
import lib.kasuga.structure.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.List;

public class PowerNode implements NbtSerializable {
    private final Pair<BlockPos,Integer> position;
    public final PowerNodeType type;
    private final List<PowerEdge> connection;

    public PowerNode(BlockPos pos, int index, ResourceLocation type){
        this.position = Pair.of(pos,index);
        this.type = AllPowerNodeTypes.get(type);
        connection = new ArrayList<>();
    }

    public PowerNode(BlockPos pos,int index,PowerNodeType type){
        this.position = Pair.of(pos,index);
        this.type = type;
        connection = new ArrayList<>();
    }

    public BlockPos getpos(){return position.getFirst();}
    public int getIndex(){return position.getSecond();}
    public Pair<BlockPos,Integer> getPositionPair(){return position;}
    public PowerNodeType getType(){return type;}
    public PowerPackage consume(PowerPackage input){return type.applyConsumer(input);}
    public PowerPackage apply(PowerPackage input){return type.applySupplier(input);}

    public boolean connectedTo(PowerNode node){
        if (node == this){return true;}
        for (PowerEdge edge : connection){
            if (edge.hasNode(node)){return true;}
        }
        return false;
    }

    public PowerEdge findConnectionWith(PowerNode node){
        for (PowerEdge edge : connection){
            if (edge.hasNode(node)){return edge;}
        }
        return null;
    }

    public List<PowerNode> getConnectedNode(){
        ArrayList<PowerNode> result = new ArrayList<>(this.connection.size());
        connection.forEach(powerEdge -> result.add(powerEdge.getNextSide(this)));
        return result;
    }

    public void connect(PowerNode node, OverheadLineType type){
        PowerEdge edge = new PowerEdge(type,this,node);
        connection.add(edge);
        node.connection.add(edge);
    }

    public boolean disConnect(PowerNode node){
        PowerEdge edge = findConnectionWith(node);
        if (edge == null){return false;}
        node.connection.remove(edge);
        this.connection.remove(edge);
        return true;
    }

    public List<PowerEdge> getConnection(){return connection;}

    @Override
    public boolean equals(Object obj) {
        if (this == obj){return true;}
        if (!(obj instanceof PowerNode node)){return false;}
        return this.position.getFirst().equals(node.position.getFirst()) && this.position.getSecond().equals(node.position.getSecond());
    }

    @Override
    public void write(CompoundTag var1) {}

    @Override
    public void read(CompoundTag var2) {}

    public static PowerNode readFromNbt(CompoundTag nbt){
        BlockPos pos = Utils.readBlockPos(nbt,"pos");
        int index = nbt.getInt("index");
        ResourceLocation type = Utils.resourceLocation(nbt,"type");
        return new PowerNode(pos,index,type);
    }
}
