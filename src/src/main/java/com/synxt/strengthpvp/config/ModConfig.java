package com.synxt.strengthpvp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.synxt.strengthpvp.feature.FeatureModule;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

public final class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("strengthpvphelper.json");
    private static Data data = new Data();

    private ModConfig() {}

    public static void load() {
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                Data loaded = GSON.fromJson(reader, Data.class);
                if (loaded != null) data = loaded;
            } catch (Exception exception) {
                System.err.println("[StrengthPvPHelper] Не удалось прочитать конфиг: " + exception.getMessage());
            }
        }
        if (data.modules == null) data.modules = new EnumMap<>(FeatureModule.class);
        for (FeatureModule module : FeatureModule.values()) {
            if (!data.modules.containsKey(module)) data.modules.put(module, new ModuleSettings());
        }
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception exception) {
            System.err.println("[StrengthPvPHelper] Не удалось сохранить конфиг: " + exception.getMessage());
        }
    }

    public static ModuleSettings get(FeatureModule module) {
        ModuleSettings settings = data.modules.get(module);
        if (settings == null) {
            settings = new ModuleSettings();
            data.modules.put(module, settings);
        }
        return settings;
    }

    public static boolean isEnabled(FeatureModule module) {
        return get(module).enabled;
    }

    public static void setEnabled(FeatureModule module, boolean enabled) {
        get(module).enabled = enabled;
        save();
    }

    public static int getArmorWarningPercent() {
        return data.armorWarningPercent;
    }

    public static void setArmorWarningPercent(int percent) {
        data.armorWarningPercent = Math.max(5, Math.min(50, percent));
        save();
    }

    public static final class Data {
        public Map<FeatureModule, ModuleSettings> modules = new EnumMap<>(FeatureModule.class);
        public int armorWarningPercent = 20;
    }

    public static final class ModuleSettings {
        public boolean enabled = false;
        public int keyCode = 0;
        public String keyType = "KEYSYM";
    }
}
