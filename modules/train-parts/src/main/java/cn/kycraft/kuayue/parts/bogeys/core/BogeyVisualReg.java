package cn.kycraft.kuayue.parts.bogeys.core;

import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import lib.kasuga.registration.Reg;
import lib.kasuga.registration.core.RegisterContext;
import lib.kasuga.registration.stages.RegistrationStage;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BogeyVisualReg<B extends BlockEntity> extends Reg<BogeyVisualReg<B>, Void> {

    private final Supplier<SimpleBlockEntityVisualizer.Factory<B>> visualFactory;
    private final Collection<Supplier<BlockEntityType<?>>> beTypeSuppliers = new HashSet<>();
    private Predicate<B> skipVanillaRender = b -> false;

    public BogeyVisualReg(Supplier<SimpleBlockEntityVisualizer.Factory<B>> visualFactory) {
        this.visualFactory = visualFactory;
    }

    public BogeyVisualReg<B> withBlockEntity(Supplier<BlockEntityType<?>> beTypeSupplier) {
        this.beTypeSuppliers.add(beTypeSupplier);
        return this;
    }

    public BogeyVisualReg<B> skipVanillaRender(Predicate<B> predicate) {
        this.skipVanillaRender = predicate;
        return this;
    }

    @Override
    public Void getEntry() {
        return null;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void register(RegisterContext<?> context) {
        context.onStage(RegistrationStage.BAKING_COMPLETE, $ -> {
            CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> {
                for (Supplier<BlockEntityType<?>> beSupplier : beTypeSuppliers) {
                    BlockEntityType<?> beType = beSupplier.get();
                    if (beType != null) {
                        SimpleBlockEntityVisualizer.builder(beType)
                                .factory((SimpleBlockEntityVisualizer.Factory) visualFactory.get())
                                .skipVanillaRender(b -> skipVanillaRender.test((B) b))
                                .apply();
                    }
                }
            });
        });
    }
}
