package com.synxt.strengthpvp.feature;

import com.synxt.strengthpvp.config.ModConfig;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.Map;

public final class ModKeybinds {
    private static final String CATEGORY = "key.categories.strengthpvp";
    private static final Map<FeatureModule, KeyBinding> MODULE_KEYS = new EnumMap<>(FeatureModule.class);
    public static KeyBinding openMenu;

    private ModKeybinds() {}

    public static void register() {
        openMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.strengthpvp.open_menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));

        for (FeatureModule module : FeatureModule.values()) {
            if (!module.implemented) continue;
            int keyCode = ModConfig.get(module).keyCode;
            InputUtil.Type keyType;
            try {
                keyType = InputUtil.Type.valueOf(ModConfig.get(module).keyType);
            } catch (Exception ignored) {
                keyType = InputUtil.Type.KEYSYM;
            }
            KeyBinding key = new KeyBinding("key.strengthpvp." + module.name().toLowerCase(),
                    keyType, keyCode, CATEGORY);
            MODULE_KEYS.put(module, KeyBindingHelper.registerKeyBinding(key));
        }
    }

    public static KeyBinding get(FeatureModule module) {
        return MODULE_KEYS.get(module);
    }

    public static void setKey(FeatureModule module, InputUtil.Key key) {
        KeyBinding binding = get(module);
        if (binding == null) return;
        binding.setBoundKey(key);
        KeyBinding.updateKeysByCode();
        ModConfig.get(module).keyCode = key.getCode();
        ModConfig.get(module).keyType = key.getCategory().name();
        ModConfig.save();
    }
}
