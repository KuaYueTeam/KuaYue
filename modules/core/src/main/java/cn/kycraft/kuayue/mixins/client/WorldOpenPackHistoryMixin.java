package cn.kycraft.kuayue.mixins.client;

import cn.kycraft.kuayue.packs.history.PackHistoryGuard;
import cn.kycraft.kuayue.packs.history.PackHistoryPreflight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Adds the single-player world-record confirmation before vanilla starts loading a world. */
@Mixin(WorldOpenFlows.class)
public abstract class WorldOpenPackHistoryMixin {
    @Unique
    private static final Set<String> KUAYUE_CONFIRMED =
            ConcurrentHashMap.newKeySet();

    @Inject(method = "openWorld", at = @At("HEAD"), cancellable = true)
    private void kuayue$confirmPackRecord(String worldName, Runnable onFail, CallbackInfo callback) {
        if (KUAYUE_CONFIRMED.remove(worldName)) return;
        Minecraft minecraft = Minecraft.getInstance();
        Screen parent = minecraft.screen;
        Path worldRoot = minecraft.gameDirectory.toPath().resolve("saves").resolve(worldName);
        PackHistoryPreflight.Result result = PackHistoryPreflight.inspect(worldRoot);
        if (result.status() == PackHistoryPreflight.Status.ERROR) {
            callback.cancel();
            minecraft.setScreen(new ConfirmScreen(
                    ignored -> minecraft.setScreen(parent),
                    Component.translatable("kuayue.packs.world_record.error.title"),
                    Component.literal(String.join("\n", result.messages()))
            ));
            return;
        }
        if (result.status() != PackHistoryPreflight.Status.CONFIRMATION_REQUIRED) return;

        callback.cancel();
        minecraft.setScreen(new ConfirmScreen(
                proceed -> {
                    if (!proceed) {
                        minecraft.setScreen(parent);
                        return;
                    }
                    PackHistoryGuard.confirmWorld(worldRoot);
                    KUAYUE_CONFIRMED.add(worldName);
                    try {
                        ((WorldOpenFlows) (Object) this).openWorld(worldName, () -> {
                            KUAYUE_CONFIRMED.remove(worldName);
                            PackHistoryGuard.cancelConfirmation(worldRoot);
                            onFail.run();
                        });
                    } catch (RuntimeException failure) {
                        KUAYUE_CONFIRMED.remove(worldName);
                        PackHistoryGuard.cancelConfirmation(worldRoot);
                        throw failure;
                    }
                },
                Component.translatable("kuayue.packs.world_record.warning.title"),
                Component.translatable(
                        "kuayue.packs.world_record.warning.body",
                        String.join("\n", result.messages())
                )
        ));
    }
}
