package com.synxt.strengthpvp.feature;

import com.synxt.strengthpvp.config.ModConfig;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import java.util.EnumMap;
import java.util.Map;

public final class ModKeybinds {
    private static final String CATEGORY = "key.categories.strengthpvp";
    private static final Map<FeatureModule, KeyBinding> MODULE_KEYS = new EnumMap<>(FeatureModule.class);
    public static KeyBinding openMenu;
    public static KeyBinding openHudEditor;

    private ModKeybinds() {}

    public static void register() {
        openMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.strengthpvp.open_menu", InputUtil.Type.KEYSYM, 344, CATEGORY));
        openHudEditor = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.strengthpvp.open_hud_editor", InputUtil.Type.KEYSYM, 296, CATEGORY));

        for (FeatureModule module : FeatureModule.values()) {
            ModConfig.ModuleSettings settings = ModConfig.get(module);
            InputUtil.Type type;
            try { type = InputUtil.Type.valueOf(settings.keyType); }
            catch (Exception ignored) { type = InputUtil.Type.KEYSYM; }
            int defaultCode = module == FeatureModule.FREE_LOOK ? 86 : 0; // V
            int boundCode = settings.keyCode == 0 ? defaultCode : settings.keyCode;
            KeyBinding key = new KeyBinding("key.strengthpvp." + module.name().toLowerCase(), type, boundCode, CATEGORY);
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
