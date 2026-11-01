package com.synxt.strengthpvp.ui;

import com.synxt.strengthpvp.config.ModConfig;
import com.synxt.strengthpvp.feature.FeatureModule;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/** Drag-and-drop HUD editor. Right click toggles a widget; wheel changes selected scale. */
public final class HudLayoutEditorScreen extends Screen {
    private FeatureModule selected;
    private boolean dragging;
    private double dragOffsetX, dragOffsetY;

    public HudLayoutEditorScreen() { this(null); }
    public HudLayoutEditorScreen(FeatureModule initial) {
        super(Text.of("StrengthPvPHelper - HUD Editor"));
        selected = initial;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        int sw = width, sh = height;
        UiDraw.roundedRect(matrices, 8, 8, sw - 16, sh - 16, 0xCC101722, 9);
        UiDraw.drawText(textRenderer, matrices, ModConfig.isEnglish() ? "HUD LAYOUT EDITOR" : "РЕДАКТОР РАСПОЛОЖЕНИЯ HUD", 20, 18, 0xFFFF667A);
        UiDraw.drawText(textRenderer, matrices, ModConfig.isEnglish()
                ? "Left drag: move  ·  Wheel: resize selected  ·  Right click: enable/disable  ·  Middle click: reset"
                : "ЛКМ + перетаскивание: переместить  ·  Колесо: размер  ·  ПКМ: вкл/выкл  ·  СКМ: сброс",
                20, 32, 0xFFB4C0D0);

        for (FeatureModule module : FeatureModule.values()) {
            if (!module.hasHudWidget()) continue;
            int x = toX(module, ModConfig.getHudX(module), sw);
            int y = toY(module, ModConfig.getHudY(module), sh);
            int scale = ModConfig.getScale(module);
            int baseW = previewWidth(module);
            int baseH = previewHeight(module);
            int w = Math.max(28, baseW * scale / 100);
            int h = Math.max(16, baseH * scale / 100);
            boolean enabled = ModConfig.isEnabled(module);
            int bg = ModConfig.getBackgroundColor(module);
            UiDraw.roundedRect(matrices, x, y, w, h, enabled ? UiDraw.withAlpha(bg, ModConfig.getModuleOpacity(module) * 255 / 100) : 0x99303A49, 6);
            UiDraw.roundedRect(matrices, x + 4, y + 3, Math.max(4, w - 8), 2,
                    ModConfig.getAccentColor(module), 1);
            String label = module.title(ModConfig.isEnglish()) + (enabled ? "" : "  [OFF]");
            UiDraw.drawText(textRenderer, matrices, label, x + 7, y + 10, ModConfig.getTextColor(module));
            if (module == selected) {
                UiDraw.roundedRect(matrices, x - 2, y - 2, w + 4, h + 4, 0x00FFFFFF, 7);
                UiDraw.roundedRect(matrices, x - 2, y - 2, w + 4, 2, 0xFFFFC45E, 2);
                UiDraw.roundedRect(matrices, x - 2, y + h, w + 4, 2, 0xFFFFC45E, 2);
                UiDraw.roundedRect(matrices, x - 2, y - 2, 2, h + 4, 0xFFFFC45E, 2);
                UiDraw.roundedRect(matrices, x + w, y - 2, 2, h + 4, 0xFFFFC45E, 2);
            }
        }

        if (selected != null) {
            String details = selected.title(ModConfig.isEnglish()) + "  ·  " + ModConfig.getScale(selected) + "%  ·  "
                    + (ModConfig.isEnabled(selected) ? (ModConfig.isEnglish() ? "ON" : "ВКЛ") : (ModConfig.isEnglish() ? "OFF" : "ВЫКЛ"));
            UiDraw.drawText(textRenderer, matrices, details, 20, sh - 30, 0xFFFFC45E);
        }
        UiDraw.drawText(textRenderer, matrices, ModConfig.isEnglish() ? "ESC — back" : "ESC — назад", sw - 95, sh - 30, 0xFF8C9AAD);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            FeatureModule hit = findWidget(mouseX, mouseY);
            if (hit != null) { selected = hit; ModConfig.setEnabled(hit, !ModConfig.isEnabled(hit)); return true; }
        }
        if (button == 2) {
            FeatureModule hit = findWidget(mouseX, mouseY);
            if (hit != null) { selected = hit; ModConfig.resetWidget(hit); return true; }
        }
        if (button == 0) {
            FeatureModule hit = findWidget(mouseX, mouseY);
            if (hit != null) {
                selected = hit;
                dragging = true;
                dragOffsetX = mouseX - toX(hit, ModConfig.getHudX(hit), width);
                dragOffsetY = mouseY - toY(hit, ModConfig.getHudY(hit), height);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && selected != null && button == 0) {
            int widgetW = previewWidth(selected) * ModConfig.getScale(selected) / 100;
            int widgetH = previewHeight(selected) * ModConfig.getScale(selected) / 100;
            int x = MathHelper.clamp((int) Math.round(mouseX - dragOffsetX), 0, Math.max(0, width - widgetW));
            int y = MathHelper.clamp((int) Math.round(mouseY - dragOffsetY), 0, Math.max(0, height - widgetH));
            ModConfig.updateHudPosition(selected, fromX(x, width), fromY(y, height));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) { if (dragging) ModConfig.save(); dragging = false; return true; }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        FeatureModule hit = findWidget(mouseX, mouseY);
        if (hit != null) selected = hit;
        if (selected != null) {
            int scale = ModConfig.getScale(selected) + (amount > 0 ? 10 : -10);
            ModConfig.setScale(selected, MathHelper.clamp(scale, 50, 200));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    private FeatureModule findWidget(double mx, double my) {
        FeatureModule[] modules = FeatureModule.values();
        for (int i = modules.length - 1; i >= 0; i--) {
            FeatureModule module = modules[i];
            if (!module.hasHudWidget()) continue;
            int x = toX(module, ModConfig.getHudX(module), width);
            int y = toY(module, ModConfig.getHudY(module), height);
            int s = ModConfig.getScale(module);
            int w = previewWidth(module) * s / 100;
            int h = previewHeight(module) * s / 100;
            if (mx >= x - 2 && mx <= x + w + 2 && my >= y - 2 && my <= y + h + 2) return module;
        }
        return null;
    }

    private int toX(FeatureModule module, int value, int screenWidth) {
        int w = previewWidth(module) * ModConfig.getScale(module) / 100;
        return MathHelper.clamp(value * screenWidth / 1000, 0, Math.max(0, screenWidth - w));
    }
    private int toY(FeatureModule module, int value, int screenHeight) {
        int h = previewHeight(module) * ModConfig.getScale(module) / 100;
        return MathHelper.clamp(value * screenHeight / 1000, 0, Math.max(0, screenHeight - h));
    }
    private int fromX(int value, int screenWidth) { return MathHelper.clamp(value * 1000 / Math.max(1, screenWidth), 0, 1000); }
    private int fromY(int value, int screenHeight) { return MathHelper.clamp(value * 1000 / Math.max(1, screenHeight), 0, 1000); }

    private int previewWidth(FeatureModule module) {
        switch (module) {
            case ITEM_COUNTER: return 164;
            case ARMOR_HUD: return 178;
            case STATUS_EFFECTS: return 154;
            case COORDINATES_HUD: case HELD_DURABILITY: return 176;
            case DIRECTION_HUD: return 150;
            case PERFORMANCE_HUD: return 166;
            case KEYSTROKES_HUD: return 88;
            case WORLD_CLOCK: case SESSION_TIMER: case COMBO_COUNTER: return 138;
            case FOOD_HUD: case SHIELD_STATUS: return 166;
            case TOTEM_HAND_ALERT: return 190;
            case ARMOR_WARNINGS: return 220;
            case BIOME_HUD: case DEATH_MARKER: case TARGET_DIRECTION: return 178;
            case INVENTORY_SLOTS_HUD: return 156;
            case ATTACK_TIMING: return 140;
            case TARGET_INFO: case ENEMY_ARMOR: return 210;
            case HIT_RANGE: return 178;
            case TOTEM_POP: return 170;
            case LOW_HEALTH_ALERT: case TOTEM_REFILL_ALERT: case QUICK_HEAL_REMINDER:
            case PEARL_READY_ALERT: case DANGER_INDICATOR: case MISCLICK_PREVENTION: return 220;
            default: return 120;
        }
    }
    private int previewHeight(FeatureModule module) {
        switch (module) {
            case ITEM_COUNTER: return 122;
            case ARMOR_HUD: return 94;
            case STATUS_EFFECTS: return 140;
            case COORDINATES_HUD: return 43;
            case DIRECTION_HUD: case PERFORMANCE_HUD: return 34;
            case KEYSTROKES_HUD: return 82;
            case HELD_DURABILITY: return 42;
            case WORLD_CLOCK: case SESSION_TIMER: case COMBO_COUNTER: return 38;
            case FOOD_HUD: return 40;
            case SHIELD_STATUS: return 38;
            case BIOME_HUD: case DEATH_MARKER: case TARGET_DIRECTION: return 38;
            case INVENTORY_SLOTS_HUD: return 38;
            case HIT_RANGE: return 42;
            case TOTEM_HAND_ALERT: return 32;
            case ARMOR_WARNINGS: return 26;
            case ATTACK_TIMING: return 30;
            case TARGET_INFO: return 50;
            case ENEMY_ARMOR: return 56;
            case TOTEM_POP: return 45;
            case LOW_HEALTH_ALERT: case TOTEM_REFILL_ALERT: case QUICK_HEAL_REMINDER:
            case PEARL_READY_ALERT: case DANGER_INDICATOR: case MISCLICK_PREVENTION: return 26;
            default: return 22;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { client.setScreen(new StrengthMenuScreen()); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean isPauseScreen() { return false; }
}
