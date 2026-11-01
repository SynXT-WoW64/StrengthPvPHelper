package com.synxt.strengthpvp;

import com.synxt.strengthpvp.config.ModConfig;
import com.synxt.strengthpvp.feature.FeatureModule;
import com.synxt.strengthpvp.feature.ModKeybinds;
import com.synxt.strengthpvp.hud.HudRenderer;
import com.synxt.strengthpvp.ui.StrengthMenuScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StrengthPvPHelper implements ClientModInitializer {
    public static final String MOD_ID = "strengthpvp";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        ModKeybinds.register();
        HudRenderer.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> handleKeys(client));
        LOGGER.info("Strength PvP Helper 1.17.1 успешно запущен!");
    }

    private void handleKeys(MinecraftClient client) {
        if (ModKeybinds.openMenu.wasPressed()) {
            if (client.currentScreen instanceof StrengthMenuScreen) client.setScreen(null);
            else client.setScreen(new StrengthMenuScreen());
        }
        if (client.currentScreen != null) return;
        for (FeatureModule module : FeatureModule.values()) {
            if (!module.implemented) continue;
            while (ModKeybinds.get(module).wasPressed()) {
                ModConfig.setEnabled(module, !ModConfig.isEnabled(module));
            }
        }
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}
