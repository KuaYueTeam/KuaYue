package cn.kycraft.kuayue.mixins;

import cn.kycraft.kuayue.packs.network.PackSetConfigurationGate;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.neoforge.network.ConfigurationInitialization;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/** Runs the pack-set gate before NeoForge starts registry synchronization. */
@Mixin(value = ConfigurationInitialization.class, remap = false)
public abstract class ConfigurationInitializationMixin {
    @Inject(method = "configureEarlyTasks", at = @At("HEAD"), remap = false)
    private static void kuayue$checkPackSetBeforeRegistrySync(
            ServerConfigurationPacketListener listener,
            Consumer<ConfigurationTask> tasks,
            CallbackInfo callback
    ) {
        tasks.accept(new PackSetConfigurationGate());
    }
}
