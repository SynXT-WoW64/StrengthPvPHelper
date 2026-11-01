package com.synxt.strengthpvp;

import com.synxt.strengthpvp.config.ModConfig;
import com.synxt.strengthpvp.feature.CombatFeatures;
import com.synxt.strengthpvp.feature.CrystalOptimiser;
import com.synxt.strengthpvp.feature.FeatureModule;
import com.synxt.strengthpvp.feature.FreeLookController;
import com.synxt.strengthpvp.feature.ModKeybinds;
import com.synxt.strengthpvp.hud.HudRenderer;
import com.synxt.strengthpvp.hud.WorldOverlayRenderer;
import com.synxt.strengthpvp.ui.StrengthMenuScreen;
import com.synxt.strengthpvp.ui.HudLayoutEditorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class StrengthPvPHelper implements ClientModInitializer {
    public static final String MOD_ID = "strengthpvp";

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        ModKeybinds.register();
        HudRenderer.register();
        WorldOverlayRenderer.register();

        ClientTickEvents.START_CLIENT_TICK.register(CombatFeatures::preTick);
        ClientTickEvents.END_CLIENT_TICK.register(CombatFeatures::tick);
        ClientTickEvents.END_CLIENT_TICK.register(CrystalOptimiser::tick);
        ClientTickEvents.END_CLIENT_TICK.register(this::handleKeys);
        ClientTickEvents.END_CLIENT_TICK.register(FreeLookController::tick);

        System.out.println("[StrengthPvPHelper] Client initialized for Minecraft 1.17.1");
    }

    private void handleKeys(MinecraftClient client) {
        if (ModKeybinds.openMenu.wasPressed()) {
            if (client.currentScreen instanceof StrengthMenuScreen) client.setScreen(null);
            else client.setScreen(new StrengthMenuScreen());
        }
        if (ModKeybinds.openHudEditor.wasPressed()) {
            if (client.currentScreen instanceof HudLayoutEditorScreen) client.setScreen(null);
            else client.setScreen(new HudLayoutEditorScreen());
        }
        if (client.currentScreen != null) {
            // Do not let key presses made inside chat/menus trigger modules after the screen closes.
            for (FeatureModule module : FeatureModule.values()) {
                while (ModKeybinds.get(module).wasPressed()) { /* discard GUI key events */ }
            }
            return;
        }

        for (FeatureModule module : FeatureModule.values()) {
            while (ModKeybinds.get(module).wasPressed()) {
                if (module == FeatureModule.JUNK_DROPPER) {
                    if (ModConfig.isEnabled(module)) CombatFeatures.dropJunk(client);
                } else {
                    ModConfig.setEnabled(module, !ModConfig.isEnabled(module));
                }
            }
        }
    }
}
