package com.synxt.strengthpvp.config;

import com.synxt.strengthpvp.feature.FeatureModule;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Dependency-free settings store. All HUD widget values are persisted per module. */
public final class ModConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("strengthpvphelper.json");
    private static final int CROSSHAIR_SIZE = 15;
    private static final Data DATA = new Data();
    private static final int[] ACCENTS = {0xFFFF667A, 0xFF64C8FF, 0xFF74DDA7, 0xFFFFC45E, 0xFFC39AFF, 0xFFF0F3F8};
    private static final int[] BACKGROUNDS = {0xE6101722, 0xE6172637, 0xE61D2431, 0xE6241D30, 0xE61E302C};
    private static final int[] TEXT_COLORS = {0xFFF0F4F9, 0xFFD8E9F7, 0xFFFFF0D0, 0xFFE7D8FF, 0xFFBFCAD8};
    private static final int[] SECONDARY_COLORS = {0xFFFF4545, 0xFFFF8A45, 0xFFC77DFF, 0xFF64C8FF, 0xFFFFC45E};
    private static final int[] TERTIARY_COLORS = {0xFF4DFF87, 0xFF32D6B3, 0xFF66BFFF, 0xFFFFC45E, 0xFFC39AFF};
    public static final int[] CROSSHAIR_PALETTE = {0xFFFFFFFF, 0xFFFF626B, 0xFF66C9FF, 0xFF72E0A4,
            0xFFFFC45E, 0xFFC39AFF, 0xFF202833, 0xFF000000};

    private ModConfig() {}

    public static void load() {
        String json = "";
        try {
            if (Files.exists(FILE)) json = new String(Files.readAllBytes(FILE), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            System.err.println("[StrengthPvPHelper] Config read failed: " + exception.getMessage());
        }
        DATA.language = readString(json, "language", "ru");
        if (!"en".equalsIgnoreCase(DATA.language)) DATA.language = "ru";
        DATA.opacityPercent = clamp(readInt(json, "opacityPercent", 85), 70, 100);
        DATA.superOptimization = readBoolean(json, "superOptimization", false);
        DATA.autoEzzText = readString(json, "autoEzzText", "ez");
        DATA.autoEzzText = sanitizeChatText(DATA.autoEzzText);
        DATA.crosshairPixels = sanitizeCrosshair(readString(json, "crosshairPixels", defaultCrosshair()), defaultCrosshair());
        DATA.crosshairColor = clamp(readInt(json, "crosshairColor", 1), 1, CROSSHAIR_PALETTE.length);

        for (FeatureModule module : FeatureModule.values()) {
            ModuleSettings s = new ModuleSettings(module);
            Pattern p = Pattern.compile("\\\"" + module.name() + "\\\"\\s*:\\s*\\{([^}]*)}", Pattern.DOTALL);
            Matcher m = p.matcher(json);
            if (m.find()) {
                String body = m.group(1);
                s.enabled = readBoolean(body, "enabled", module.defaultEnabled);
                s.keyCode = readInt(body, "keyCode", 0);
                s.keyType = readString(body, "keyType", "KEYSYM");
                s.optionValue = readInt(body, "optionValue", module.defaultOption);
                s.optionValue2 = readInt(body, "optionValue2", defaultOption2(module));
                s.optionValue3 = readInt(body, "optionValue3", defaultOption3(module));
                s.optionValue4 = readInt(body, "optionValue4", defaultOption4(module));
                s.optionValue5 = readInt(body, "optionValue5", defaultOption5(module));
                s.optionValue6 = readInt(body, "optionValue6", defaultOption6(module));
                s.optionValue7 = readInt(body, "optionValue7", defaultOption7(module));
                s.optionValue8 = readInt(body, "optionValue8", defaultOption8(module));
                s.optionValue9 = readInt(body, "optionValue9", defaultOption9(module));
                s.optionValue10 = readInt(body, "optionValue10", defaultOption10(module));
                s.optionValue11 = readInt(body, "optionValue11", defaultOption11(module));
                s.hudX = readInt(body, "hudX", s.hudX);
                s.hudY = readInt(body, "hudY", s.hudY);
                s.scalePercent = readInt(body, "scalePercent", 100);
                s.opacityPercent = readInt(body, "opacityPercent", 100);
                s.accentColor = readInt(body, "accentColor", s.accentColor);
                s.backgroundColor = readInt(body, "backgroundColor", s.backgroundColor);
                s.textColor = readInt(body, "textColor", s.textColor);
                s.secondaryColor = readInt(body, "secondaryColor", s.secondaryColor);
                s.tertiaryColor = readInt(body, "tertiaryColor", s.tertiaryColor);
            }
            s.hudX = clamp(s.hudX, 0, 1000);
            s.hudY = clamp(s.hudY, 0, 1000);
            s.scalePercent = clamp(s.scalePercent, 50, 300);
            s.opacityPercent = clamp(s.opacityPercent, 10, 100);
            if (module == FeatureModule.BETTER_PVP) {
                s.optionValue4 = clamp(s.optionValue4, -45, 45);  // tilt degrees
                s.optionValue5 = clamp(s.optionValue5, -50, 50);  // depth, hundredths
                s.optionValue6 = clamp(s.optionValue6, 50, 130);  // hand scale %
                s.optionValue7 = clamp(s.optionValue7, 0, 4);    // animation preset
                s.optionValue8 = clamp(s.optionValue8, 1, 5);    // custom curve power
                s.optionValue9 = clamp(s.optionValue9, 50, 150); // animation speed %
                s.optionValue10 = clamp(s.optionValue10, -45, 45); // animated hand tilt degrees
                s.optionValue11 = clamp(s.optionValue11, -30, 30); // animated horizontal shift, hundredths
            }
            if (module.hasOption()) s.optionValue = clamp(s.optionValue, module.minOption, module.maxOption);
            if (module == FeatureModule.BETTER_PVP) {
                s.optionValue2 = clamp(s.optionValue2, -30, 30); // horizontal hand offset, tenths
                s.optionValue3 = clamp(s.optionValue3, -30, 30); // vertical hand offset, tenths
            }
            DATA.modules.put(module, s);
        }
        save();
    }

    public static void save() {
        StringBuilder out = new StringBuilder(4096);
        out.append("{\n  \"language\": \"").append(DATA.language).append("\",\n");
        out.append("  \"opacityPercent\": ").append(DATA.opacityPercent).append(",\n");
        out.append("  \"superOptimization\": ").append(DATA.superOptimization).append(",\n");
        out.append("  \"autoEzzText\": \"").append(jsonEscape(DATA.autoEzzText)).append("\",\n");
        out.append("  \"crosshairPixels\": \"").append(DATA.crosshairPixels).append("\",\n");
        out.append("  \"crosshairColor\": ").append(DATA.crosshairColor).append(",\n");
        out.append("  \"modules\": {\n");
        int index = 0;
        for (FeatureModule module : FeatureModule.values()) {
            ModuleSettings s = get(module);
            out.append("    \"").append(module.name()).append("\": {")
                    .append("\"enabled\": ").append(s.enabled)
                    .append(", \"keyCode\": ").append(s.keyCode)
                    .append(", \"keyType\": \"").append(s.keyType)
                    .append("\", \"optionValue\": ").append(s.optionValue)
                    .append(", \"optionValue2\": ").append(s.optionValue2)
                    .append(", \"optionValue3\": ").append(s.optionValue3)
                    .append(", \"optionValue4\": ").append(s.optionValue4)
                    .append(", \"optionValue5\": ").append(s.optionValue5)
                    .append(", \"optionValue6\": ").append(s.optionValue6)
                    .append(", \"optionValue7\": ").append(s.optionValue7)
                    .append(", \"optionValue8\": ").append(s.optionValue8)
                    .append(", \"optionValue9\": ").append(s.optionValue9)
                    .append(", \"optionValue10\": ").append(s.optionValue10)
                    .append(", \"optionValue11\": ").append(s.optionValue11)
                    .append(", \"hudX\": ").append(s.hudX)
                    .append(", \"hudY\": ").append(s.hudY)
                    .append(", \"scalePercent\": ").append(s.scalePercent)
                    .append(", \"opacityPercent\": ").append(s.opacityPercent)
                    .append(", \"accentColor\": ").append(s.accentColor)
                    .append(", \"backgroundColor\": ").append(s.backgroundColor)
                    .append(", \"textColor\": ").append(s.textColor)
                    .append(", \"secondaryColor\": ").append(s.secondaryColor)
                    .append(", \"tertiaryColor\": ").append(s.tertiaryColor).append("}");
            if (++index < FeatureModule.values().length) out.append(',');
            out.append('\n');
        }
        out.append("  }\n}\n");
        Path temporary = FILE.resolveSibling(FILE.getFileName().toString() + ".tmp");
        try {
            Files.createDirectories(FILE.getParent());
            Files.write(temporary, out.toString().getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveNotSupported) {
                Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            System.err.println("[StrengthPvPHelper] Config write failed: " + exception.getMessage());
        }
    }

    private static int defaultOption2(FeatureModule m) { return 0; }
    private static int defaultOption3(FeatureModule m) { return 0; }
    private static int defaultOption4(FeatureModule m) { return 0; }
    private static int defaultOption5(FeatureModule m) { return 0; }
    private static int defaultOption6(FeatureModule m) { return 100; }
    private static int defaultOption7(FeatureModule m) { return 0; }
    private static int defaultOption8(FeatureModule m) { return 3; }
    private static int defaultOption9(FeatureModule m) { return 100; }
    private static int defaultOption10(FeatureModule m) { return 0; }
    private static int defaultOption11(FeatureModule m) { return 0; }

    public static ModuleSettings get(FeatureModule module) {
        ModuleSettings s = DATA.modules.get(module);
        if (s == null) { s = new ModuleSettings(module); DATA.modules.put(module, s); }
        return s;
    }
    public static boolean isEnabled(FeatureModule m) { return get(m).enabled; }
    public static void setEnabled(FeatureModule m, boolean v) { get(m).enabled = v; save(); }
    public static int getOption(FeatureModule m) { return get(m).optionValue; }
    public static void setOption(FeatureModule m, int v) { updateOption(m, v); save(); }
    public static void updateOption(FeatureModule m, int v) { if (m.hasOption()) get(m).optionValue = clamp(v, m.minOption, m.maxOption); }
    public static int getOption2(FeatureModule m) { return get(m).optionValue2; }
    public static void setOption2(FeatureModule m, int v) { updateOption2(m, v); save(); }
    public static void updateOption2(FeatureModule m, int v) {
        if (m == FeatureModule.BETTER_PVP) v = clamp(v, -30, 30);
        get(m).optionValue2 = v;
    }
    public static int getOption3(FeatureModule m) { return get(m).optionValue3; }
    public static void setOption3(FeatureModule m, int v) { updateOption3(m, v); save(); }
    public static void updateOption3(FeatureModule m, int v) {
        if (m == FeatureModule.BETTER_PVP) v = clamp(v, -30, 30);
        get(m).optionValue3 = v;
    }
    public static int getOption4(FeatureModule m) { return get(m).optionValue4; }
    public static int getOption5(FeatureModule m) { return get(m).optionValue5; }
    public static int getOption6(FeatureModule m) { return get(m).optionValue6; }
    public static int getOption7(FeatureModule m) { return get(m).optionValue7; }
    public static int getOption8(FeatureModule m) { return get(m).optionValue8; }
    public static int getOption9(FeatureModule m) { return get(m).optionValue9; }
    public static int getOption10(FeatureModule m) { return get(m).optionValue10; }
    public static int getOption11(FeatureModule m) { return get(m).optionValue11; }
    public static void setOption4(FeatureModule m, int v) { updateOption4(m, v); save(); }
    public static void updateOption4(FeatureModule m, int v) { get(m).optionValue4 = clamp(v, -45, 45); }
    public static void setOption5(FeatureModule m, int v) { updateOption5(m, v); save(); }
    public static void updateOption5(FeatureModule m, int v) { get(m).optionValue5 = clamp(v, -50, 50); }
    public static void setOption6(FeatureModule m, int v) { updateOption6(m, v); save(); }
    public static void updateOption6(FeatureModule m, int v) { get(m).optionValue6 = clamp(v, 50, 130); }
    public static void setOption7(FeatureModule m, int v) { updateOption7(m, v); save(); }
    public static void updateOption7(FeatureModule m, int v) { get(m).optionValue7 = clamp(v, 0, 4); }
    public static void setOption8(FeatureModule m, int v) { updateOption8(m, v); save(); }
    public static void updateOption8(FeatureModule m, int v) { get(m).optionValue8 = clamp(v, 1, 5); }
    public static void setOption9(FeatureModule m, int v) { updateOption9(m, v); save(); }
    public static void updateOption9(FeatureModule m, int v) { get(m).optionValue9 = clamp(v, 50, 150); }
    public static void setOption10(FeatureModule m, int v) { updateOption10(m, v); save(); }
    public static void updateOption10(FeatureModule m, int v) { get(m).optionValue10 = clamp(v, -45, 45); }
    public static void setOption11(FeatureModule m, int v) { updateOption11(m, v); save(); }
    public static void updateOption11(FeatureModule m, int v) { get(m).optionValue11 = clamp(v, -30, 30); }
    public static void applyBetterPvpTemplate() {
        ModuleSettings s = get(FeatureModule.BETTER_PVP);
        s.optionValue = 85;
        s.optionValue2 = 4;
        s.optionValue3 = -3;
        s.optionValue4 = 16;
        s.optionValue5 = -28;
        s.optionValue6 = 78;
        s.optionValue7 = 0;
        s.optionValue8 = 4;
        s.optionValue9 = 85;
        s.optionValue10 = 18;
        s.optionValue11 = 5;
        save();
    }
    public static String getAnimationPresetName(int preset, boolean english) {
        switch (clamp(preset, 0, 4)) {
            case 1: return english ? "Vanilla" : "Ванильная";
            case 2: return english ? "Soft / sine" : "Мягкая / синус";
            case 3: return english ? "Quick / responsive" : "Быстрая / резкая";
            case 4: return english ? "Custom curve" : "Своя кривая";
            default: return english ? "Smooth" : "Плавная";
        }
    }
    public static int getArmorWarningPercent() { return getOption(FeatureModule.ARMOR_HUD); }
    public static void setArmorWarningPercent(int v) { setOption(FeatureModule.ARMOR_HUD, v); }
    public static boolean isEnglish() { return "en".equalsIgnoreCase(DATA.language); }
    public static String getLanguage() { return DATA.language; }
    public static void toggleLanguage() { DATA.language = isEnglish() ? "ru" : "en"; save(); }
    public static int getOpacityPercent() { return DATA.opacityPercent; }
    public static void cycleOpacity() { DATA.opacityPercent += 5; if (DATA.opacityPercent > 95) DATA.opacityPercent = 70; save(); }
    public static boolean isSuperOptimization() { return DATA.superOptimization; }
    public static void setSuperOptimization(boolean enabled) { DATA.superOptimization = enabled; save(); }
    public static String getAutoEzzText() { return DATA.autoEzzText; }
    public static void setAutoEzzText(String value) { updateAutoEzzText(value); save(); }
    public static void updateAutoEzzText(String value) { DATA.autoEzzText = sanitizeChatText(value); }
    private static String sanitizeChatText(String value) {
        if (value == null) return "ez";
        String clean = value.replace('\n', ' ').replace('\r', ' ').replace('\"', '\'').trim();
        if (clean.length() > 100) clean = clean.substring(0, 100);
        return clean;
    }
    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static int getHudX(FeatureModule m) { return get(m).hudX; }
    public static int getHudY(FeatureModule m) { return get(m).hudY; }
    public static void updateHudPosition(FeatureModule m, int x, int y) {
        ModuleSettings s = get(m);
        s.hudX = clamp(x, 0, 1000);
        s.hudY = clamp(y, 0, 1000);
    }
    public static void setHudPosition(FeatureModule m, int x, int y) {
        updateHudPosition(m, x, y);
        save();
    }
    public static int getScale(FeatureModule m) { return get(m).scalePercent; }
    public static void setScale(FeatureModule m, int p) { updateScale(m, p); save(); }
    public static void updateScale(FeatureModule m, int p) { get(m).scalePercent = clamp(p,50,300); }
    public static int getModuleOpacity(FeatureModule m) { return get(m).opacityPercent; }
    public static void setModuleOpacity(FeatureModule m, int p) { updateModuleOpacity(m, p); save(); }
    public static void updateModuleOpacity(FeatureModule m, int p) { get(m).opacityPercent = clamp(p,10,100); }
    public static int getAccentColor(FeatureModule m) { return get(m).accentColor; }
    public static int getBackgroundColor(FeatureModule m) { return get(m).backgroundColor; }
    public static int getTextColor(FeatureModule m) { return get(m).textColor; }
    public static int getSecondaryColor(FeatureModule m) { return get(m).secondaryColor; }
    public static int getTertiaryColor(FeatureModule m) { return get(m).tertiaryColor; }
    public static void cycleSecondaryColor(FeatureModule m) { get(m).secondaryColor = nextColor(SECONDARY_COLORS, get(m).secondaryColor); save(); }
    public static void cycleTertiaryColor(FeatureModule m) { get(m).tertiaryColor = nextColor(TERTIARY_COLORS, get(m).tertiaryColor); save(); }
    public static void cycleAccentColor(FeatureModule m) { get(m).accentColor = nextColor(ACCENTS, get(m).accentColor); save(); }
    public static void cycleBackgroundColor(FeatureModule m) { get(m).backgroundColor = nextColor(BACKGROUNDS, get(m).backgroundColor); save(); }
    public static void cycleTextColor(FeatureModule m) { get(m).textColor = nextColor(TEXT_COLORS, get(m).textColor); save(); }
    private static int nextColor(int[] colors, int old) { for (int i=0;i<colors.length;i++) if (colors[i] == old) return colors[(i+1)%colors.length]; return colors[0]; }

    public static void resetWidget(FeatureModule m) {
        ModuleSettings s = get(m);
        int[] pos = defaultPosition(m);
        s.hudX = pos[0]; s.hudY = pos[1]; s.scalePercent = 100; s.opacityPercent = 100;
        s.accentColor = ACCENTS[0]; s.backgroundColor = BACKGROUNDS[0]; s.textColor = TEXT_COLORS[0];
        s.secondaryColor = SECONDARY_COLORS[0]; s.tertiaryColor = TERTIARY_COLORS[0];
        save();
    }

    public static int getCrosshairSize() { return CROSSHAIR_SIZE; }
    public static String getCrosshairPixels() { return DATA.crosshairPixels; }
    public static int getCrosshairColor() { return DATA.crosshairColor; }
    public static int getCrosshairPaletteColor(int index) {
        return CROSSHAIR_PALETTE[clamp(index - 1, 0, CROSSHAIR_PALETTE.length - 1)];
    }
    public static void setCrosshairColor(int color) { DATA.crosshairColor = clamp(color, 1, CROSSHAIR_PALETTE.length); save(); }
    public static void setCrosshairPixel(int x, int y, int colorIndex) {
        paintCrosshairPixel(x, y, colorIndex);
        save();
    }
    public static void paintCrosshairPixel(int x, int y, int colorIndex) {
        if (x < 0 || y < 0 || x >= CROSSHAIR_SIZE || y >= CROSSHAIR_SIZE) return;
        char[] pixels = sanitizeCrosshair(DATA.crosshairPixels, defaultCrosshair()).toCharArray();
        pixels[y * CROSSHAIR_SIZE + x] = (char) ('0' + clamp(colorIndex, 0, CROSSHAIR_PALETTE.length));
        DATA.crosshairPixels = new String(pixels);
    }
    public static void setCrosshairPixels(String pixels) {
        DATA.crosshairPixels = sanitizeCrosshair(pixels, defaultCrosshair());
        save();
    }
    public static void clearCrosshair() { DATA.crosshairPixels = repeat('0', CROSSHAIR_SIZE * CROSSHAIR_SIZE); save(); }
    public static void resetCrosshair() { DATA.crosshairPixels = defaultCrosshair(); DATA.crosshairColor = 1; save(); }
    private static String sanitizeCrosshair(String value, String fallback) {
        if (value == null || value.length() != CROSSHAIR_SIZE * CROSSHAIR_SIZE) return fallback;
        for (int i = 0; i < value.length(); i++) if (value.charAt(i) < '0' || value.charAt(i) > (char) ('0' + CROSSHAIR_PALETTE.length)) return fallback;
        return value;
    }
    private static String defaultCrosshair() {
        char[] pixels = repeat('0', CROSSHAIR_SIZE * CROSSHAIR_SIZE).toCharArray();
        int c = CROSSHAIR_SIZE / 2;
        for (int i = 3; i <= 11; i++) {
            if (i < 6 || i > 8) {
                pixels[c * CROSSHAIR_SIZE + i] = '1';
                pixels[i * CROSSHAIR_SIZE + c] = '1';
            }
        }
        pixels[c * CROSSHAIR_SIZE + c] = '1';
        return new String(pixels);
    }
    private static String repeat(char value, int count) {
        char[] chars = new char[count];
        java.util.Arrays.fill(chars, value);
        return new String(chars);
    }

    private static int[] defaultPosition(FeatureModule m) {
        switch (m) {
            case ARMOR_HUD: return new int[]{10, 300};
            case COORDINATES_HUD: return new int[]{10, 180};
            case DIRECTION_HUD: return new int[]{10, 245};
            case PERFORMANCE_HUD: return new int[]{820, 185};
            case KEYSTROKES_HUD: return new int[]{10, 760};
            case HELD_DURABILITY: return new int[]{820, 245};
            case WORLD_CLOCK: return new int[]{820, 310};
            case SESSION_TIMER: return new int[]{820, 365};
            case FOOD_HUD: return new int[]{10, 420};
            case BIOME_HUD: return new int[]{10, 470};
            case DEATH_MARKER: return new int[]{10, 520};
            case INVENTORY_SLOTS_HUD: return new int[]{820, 420};
            case SHIELD_STATUS: return new int[]{820, 470};
            case HIT_RANGE: return new int[]{405, 650};
            case TOTEM_HAND_ALERT: return new int[]{405, 825};
            case COMBO_COUNTER: return new int[]{405, 695};
            case TARGET_DIRECTION: return new int[]{405, 740};
            case ARMOR_WARNINGS: return new int[]{405, 785};
            case STATUS_EFFECTS: return new int[]{820, 12};
            case ATTACK_TIMING: return new int[]{420, 545};
            case TARGET_INFO: return new int[]{405, 605};
            case ENEMY_ARMOR: return new int[]{390, 385};
            case TOTEM_POP: return new int[]{400, 15};
            case LOW_HEALTH_ALERT: return new int[]{400, 440};
            case TOTEM_REFILL_ALERT: return new int[]{400, 485};
            case QUICK_HEAL_REMINDER: return new int[]{400, 530};
            case PEARL_READY_ALERT: return new int[]{400, 575};
            case DANGER_INDICATOR: return new int[]{400, 620};
            case MISCLICK_PREVENTION: return new int[]{400, 665};
            default: return new int[]{10, 12};
        }
    }

    private static int readInt(String src, String key, int def) { Matcher m=Pattern.compile("\\\""+key+"\\\"\\s*:\\s*(-?\\d+)").matcher(src); return m.find()?parseInt(m.group(1),def):def; }
    private static String readString(String src, String key, String def) { Matcher m=Pattern.compile("\\\""+key+"\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(src); return m.find()?m.group(1):def; }
    private static boolean readBoolean(String src, String key, boolean def) { Matcher m=Pattern.compile("\\\""+key+"\\\"\\s*:\\s*(true|false)",Pattern.CASE_INSENSITIVE).matcher(src); return m.find()?Boolean.parseBoolean(m.group(1)):def; }
    private static int parseInt(String s,int d){try{return Integer.parseInt(s);}catch(Exception ignored){return d;}}
    private static int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}

    private static final class Data {
        String language="ru"; int opacityPercent=85; boolean superOptimization=false; String autoEzzText="ez";
        String crosshairPixels=defaultCrosshair(); int crosshairColor=1;
        Map<FeatureModule,ModuleSettings> modules=new EnumMap<>(FeatureModule.class);
    }
    public static final class ModuleSettings {
        public boolean enabled; public int keyCode; public String keyType="KEYSYM";
        public int optionValue, optionValue2, optionValue3, optionValue4, optionValue5, optionValue6=100, optionValue7, optionValue8=3, optionValue9=100, optionValue10, optionValue11;
        public int hudX, hudY, scalePercent=100, opacityPercent=100;
        public int accentColor=ACCENTS[0], backgroundColor=BACKGROUNDS[0], textColor=TEXT_COLORS[0];
        public int secondaryColor=SECONDARY_COLORS[0], tertiaryColor=TERTIARY_COLORS[0];
        ModuleSettings(FeatureModule m) { enabled=m.defaultEnabled; optionValue=m.defaultOption; optionValue2=defaultOption2(m); optionValue3=defaultOption3(m); optionValue4=defaultOption4(m); optionValue5=defaultOption5(m); optionValue6=defaultOption6(m); optionValue7=defaultOption7(m); optionValue8=defaultOption8(m); optionValue9=defaultOption9(m); int[] p=defaultPosition(m); hudX=p[0]; hudY=p[1]; }
    }
}
