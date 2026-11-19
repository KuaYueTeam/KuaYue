package cn.kycraft.kuayue.client.settings;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Common lifecycle wrapper for a client-only LDLib2 settings screen. */
class SettingsScreen extends ModularUIScreen {
    private final Runnable back;
    private boolean released;

    SettingsScreen(ModularUI ui, Component title, Runnable back) {
        super(ui, title);
        this.back = back;
        ui.setTickWhileRending(false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!released) modularUI.tick();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        modularUI.clearFocus();
        back.run();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void removed() {
        if (!released) {
            released = true;
            try {
                modularUI.clearFocus();
                modularUI.onRemoved();
            } finally {
                modularUI.setScreen(null);
            }
        }
        super.removed();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // This is a normal Screen, so restore vanilla's full-screen backdrop. The UI root itself
        // remains transparent; world/3D UI hosts are the only contexts that should suppress it.
        if (minecraft.level == null) renderPanorama(graphics, partialTick);
        renderBlurredBackground(partialTick);
        renderMenuBackground(graphics);
    }
}
