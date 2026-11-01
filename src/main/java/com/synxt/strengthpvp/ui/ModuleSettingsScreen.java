package com.synxt.strengthpvp.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.synxt.strengthpvp.config.ModConfig;
import com.synxt.strengthpvp.feature.FeatureModule;
import com.synxt.strengthpvp.feature.ModKeybinds;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/** Clear per-module controls: exact-value sliders, common appearance controls and dedicated editors. */
public final class ModuleSettingsScreen extends Screen {
    private final FeatureModule module;
    private int panelX, panelY, panelW, panelH, viewTop, viewBottom;
    private double scroll;
    private SettingRow draggingSlider;
    private FeatureModule bindingModule;
    private String autoEzzText = "";
    private boolean autoEzzFocused;

    public ModuleSettingsScreen(FeatureModule module) {
        super(Text.of("StrengthPvPHelper - " + module.title(false)));
        this.module = module;
    }

    @Override
    protected void init() {
        super.init();
        panelW = Math.min(560, width - 20);
        panelH = Math.min(590, height - 16);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        if (module == FeatureModule.AUTO_EZZ) {
            autoEzzText = ModConfig.getAutoEzzText();
            autoEzzFocused = false;
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        panelW = Math.min(560, width - 20);
        panelH = Math.min(590, height - 16);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        int alpha = Math.round(255.0f * ModConfig.getOpacityPercent() / 100.0f);
        UiDraw.roundedRect(matrices, panelX, panelY, panelW, panelH, UiDraw.withAlpha(0x101722, alpha), 10);
        UiDraw.roundedRect(matrices, panelX, panelY, panelW, 38, UiDraw.withAlpha(0x1B293B, Math.min(255, alpha + 10)), 10);
        UiDraw.roundedRect(matrices, panelX, panelY + 25, panelW, 13, UiDraw.withAlpha(0x1B293B, Math.min(255, alpha + 10)), 0);

        boolean en = ModConfig.isEnglish();
        UiDraw.drawText(textRenderer, matrices, module.title(en), panelX + 15, panelY + 14, ModConfig.getAccentColor(module));
        UiDraw.drawText(textRenderer, matrices, en ? "SIMPLE SETTINGS" : "ПОНЯТНЫЕ НАСТРОЙКИ",
                panelX + panelW - (en ? 132 : 150), panelY + 14, 0xFF93A1B4);
        String description = textRenderer.trimToWidth(module.description(en), panelW - 32);
        UiDraw.drawText(textRenderer, matrices, description, panelX + 16, panelY + 47, 0xFFAAB8C9);

        List<SettingRow> rows = buildRows(en);
        viewTop = module == FeatureModule.AUTO_EZZ ? panelY + 130 : panelY + 70;
        viewBottom = panelY + panelH - 28;
        int totalHeight = totalHeight(rows);
        double maxScroll = Math.max(0, totalHeight - (viewBottom - viewTop));
        scroll = MathHelper.clamp(scroll, 0.0, maxScroll);

        if (module == FeatureModule.AUTO_EZZ) {
            UiDraw.drawText(textRenderer, matrices,
                    en ? "Custom message · %player% inserts the opponent name" : "Свой текст · %player% подставляет ник противника",
                    panelX + 16, panelY + 68, 0xFFE7EEF7);
            UiDraw.roundedRect(matrices, panelX + 15, panelY + 85, panelW - 30, 25,
                    autoEzzFocused ? 0xFF263A51 : 0xFF1A2635, 5);
            UiDraw.roundedRect(matrices, panelX + 15, panelY + 85, panelW - 30, 25,
                    autoEzzFocused ? 0xFFFF6874 : 0xFF3A4B60, 1);
            String shown = autoEzzText.length() > 90 ? autoEzzText.substring(0, 90) : autoEzzText;
            UiDraw.drawText(textRenderer, matrices, shown, panelX + 23, panelY + 93, 0xFFF2F5F9);
            if (autoEzzFocused) {
                int cursorX = panelX + 23 + Math.min(panelW - 52, shown.length() * 6);
                UiDraw.roundedRect(matrices, cursorX, panelY + 91, 1, 11, 0xFFFF6874, 0);
            }
        }
        UiDraw.drawText(textRenderer, matrices,
                en ? "Drag a slider · mouse wheel changes by 1 · auto-save" : "Перетаскивай ползунок · колесо меняет на 1 · автосохранение",
                panelX + 16, panelY + panelH - 17, 0xFF8493A7);

        scissor(viewTop, viewBottom);
        int y = viewTop - (int) scroll;
        for (SettingRow row : rows) {
            drawRow(matrices, row, y, mouseX, mouseY, en);
            y += row.height;
        }
        RenderSystem.disableScissor();
        if (maxScroll > 0) {
            int trackX = panelX + panelW - 7;
            UiDraw.roundedRect(matrices, trackX, viewTop, 2, viewBottom - viewTop, 0xFF344356, 1);
            int thumbH = Math.max(20, (int) ((viewBottom - viewTop) * ((viewBottom - viewTop) / (double) totalHeight)));
            int thumbY = viewTop + (int) ((viewBottom - viewTop - thumbH) * (scroll / maxScroll));
            UiDraw.roundedRect(matrices, trackX, thumbY, 2, thumbH, 0xFF9EADBF, 1);
        }
        super.render(matrices, mouseX, mouseY, delta);
    }

    private List<SettingRow> buildRows(boolean en) {
        List<SettingRow> rows = new ArrayList<>();
        rows.add(SettingRow.toggle("enabled", en ? "Module enabled" : "Модуль включён", ModConfig.isEnabled(module)));
        rows.add(SettingRow.action("bind", en ? "Change module key" : "Назначить клавишу модуля",
                bindingModule == module ? (en ? "Press a key…" : "Нажми клавишу…") : keyName()));

        if (module.hasOption()) {
            String unit = optionUnit();
            rows.add(SettingRow.slider("option", module.optionLabel(en), module.minOption, module.maxOption,
                    ModConfig.getOption(module), unit, optionFormat()));
        }

        if (module == FeatureModule.BETTER_PVP) {
            rows.add(SettingRow.slider("better_x", en ? "Hand position · left / right" : "Положение руки · влево / вправо",
                    -30, 30, ModConfig.getOption2(module), en ? "units" : "ед.", Format.DECIMAL_2));
            rows.add(SettingRow.slider("better_y", en ? "Hand position · up / down" : "Положение руки · вверх / вниз",
                    -30, 30, ModConfig.getOption3(module), en ? "units" : "ед.", Format.DECIMAL_2));
            rows.add(SettingRow.slider("better_tilt", en ? "Hand tilt" : "Наклон руки",
                    -45, 45, ModConfig.getOption4(module), "°", Format.INTEGER));
            rows.add(SettingRow.slider("better_depth", en ? "Hand distance from camera" : "Удаление руки от камеры",
                    -50, 50, ModConfig.getOption5(module), en ? "units" : "ед.", Format.DECIMAL_2));
            rows.add(SettingRow.slider("better_scale", en ? "Hand size" : "Размер руки",
                    50, 130, ModConfig.getOption6(module), "%", Format.INTEGER));
            rows.add(SettingRow.slider("better_animation", en ? "Hit animation style" : "Стиль анимации удара",
                    0, 4, ModConfig.getOption7(module), "", Format.ANIMATION));
            rows.add(SettingRow.slider("better_curve", en ? "Custom curve smoothness" : "Плавность своей кривой",
                    1, 5, ModConfig.getOption8(module), "", Format.INTEGER));
            rows.add(SettingRow.slider("better_speed", en ? "Custom animation speed" : "Скорость своей анимации",
                    50, 150, ModConfig.getOption9(module), "%", Format.INTEGER));
            rows.add(SettingRow.slider("better_swing_tilt", en ? "Animated tilt during the swing" : "Дополнительный наклон во время замаха",
                    -45, 45, ModConfig.getOption10(module), "°", Format.INTEGER));
            rows.add(SettingRow.slider("better_swing_shift", en ? "Animated hand shift" : "Сдвиг руки во время замаха",
                    -30, 30, ModConfig.getOption11(module), en ? "units" : "ед.", Format.DECIMAL_2));
            rows.add(SettingRow.action("better_template", en ? "Apply small distant smooth-hand preset" : "Применить шаблон: маленькая дальняя плавная рука",
                    en ? "Recommended" : "Рекомендуется"));
        }

        if (module == FeatureModule.CROSSHAIR_CUSTOM) {
            rows.add(SettingRow.action("crosshair_paint", en ? "Open pixel Paint editor" : "Открыть попиксельный Paint",
                    en ? "15 × 15 canvas" : "Холст 15 × 15"));
            rows.add(SettingRow.slider("scale", en ? "Crosshair pixel size" : "Размер пикселя прицела",
                    50, 300, ModConfig.getScale(module), "%", Format.INTEGER));
        }

        if (module.hasHudWidget()) {
            rows.add(SettingRow.slider("scale", en ? "Widget size" : "Размер виджета",
                    50, 300, ModConfig.getScale(module), "%", Format.INTEGER));
            rows.add(SettingRow.slider("opacity", en ? "Widget opacity" : "Непрозрачность виджета",
                    10, 100, ModConfig.getModuleOpacity(module), "%", Format.INTEGER));
            rows.add(SettingRow.action("hud_editor", en ? "Move widget on screen" : "Переместить виджет на экране",
                    en ? "Open HUD editor" : "Открыть редактор HUD"));
        }

        boolean styled = module.hasHudWidget() || module == FeatureModule.CROSSHAIR_CUSTOM;
        if (styled) {
            String clickHint = en ? "Click to change" : "Нажми, чтобы сменить";
            rows.add(SettingRow.color("accent", en ? "Accent color" : "Основной цвет", ModConfig.getAccentColor(module), clickHint));
            rows.add(SettingRow.color("background", en ? "Panel color" : "Цвет панели", ModConfig.getBackgroundColor(module), clickHint));
            rows.add(SettingRow.color("text", en ? "Text color" : "Цвет текста", ModConfig.getTextColor(module), clickHint));
            rows.add(SettingRow.color("secondary", en ? "Warning / secondary color" : "Цвет предупреждений", ModConfig.getSecondaryColor(module), clickHint));
            rows.add(SettingRow.color("tertiary", en ? "Extra / range color" : "Дополнительный цвет", ModConfig.getTertiaryColor(module), clickHint));
        }
        if (module.hasHudWidget()) {
            rows.add(SettingRow.action("reset_widget", en ? "Reset widget position and style" : "Сбросить положение и оформление",
                    en ? "Restore defaults" : "Вернуть исходные значения"));
        }
        return rows;
    }

    private void drawRow(MatrixStack matrices, SettingRow row, int y, int mouseX, int mouseY, boolean en) {
        if (y + row.height < viewTop || y > viewBottom) return;
        int x = panelX + 14;
        int w = panelW - 28;
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + row.height;
        int base = hovered ? 0xFF2A3B50 : 0xFF202F41;
        if (row.type == RowType.SLIDER) {
            UiDraw.roundedRect(matrices, x, y + 1, w, row.height - 3, base, 5);
            String value = formatValue(row);
            UiDraw.drawText(textRenderer, matrices, row.label, x + 10, y + 5, 0xFFE8EEF6);
            int valueWidth = textRenderer.getWidth(value);
            UiDraw.drawText(textRenderer, matrices, value, x + w - valueWidth - 10, y + 5, 0xFFFFC45E);
            int trackX = x + 12;
            int trackW = w - 24;
            int trackY = y + 26;
            UiDraw.roundedRect(matrices, trackX, trackY, trackW, 4, 0xFF101923, 2);
            float progress = (row.value - row.min) / (float) Math.max(1, row.max - row.min);
            int fillW = Math.max(0, Math.min(trackW, Math.round(trackW * progress)));
            UiDraw.roundedRect(matrices, trackX, trackY, fillW, 4, ModConfig.getAccentColor(module), 2);
            int knobX = trackX + fillW - 3;
            UiDraw.roundedRect(matrices, knobX, trackY - 3, 7, 10, 0xFFE9F0F7, 3);
        } else {
            UiDraw.roundedRect(matrices, x, y + 1, w, row.height - 3, base, 5);
            UiDraw.drawText(textRenderer, matrices, row.label, x + 10, y + 8, 0xFFE8EEF6);
            if (row.type == RowType.TOGGLE) {
                String state = row.value != 0 ? (en ? "ON" : "ВКЛ") : (en ? "OFF" : "ВЫКЛ");
                int pillColor = row.value != 0 ? 0xFF2D7559 : 0xFF60404A;
                UiDraw.roundedRect(matrices, x + w - 64, y + 5, 52, 18, pillColor, 5);
                int tx = x + w - 64 + (52 - textRenderer.getWidth(state)) / 2;
                UiDraw.drawText(textRenderer, matrices, state, tx, y + 10, 0xFFFFFFFF);
            } else if (row.type == RowType.ACTION) {
                int tw = textRenderer.getWidth(row.detail);
                UiDraw.drawText(textRenderer, matrices, row.detail, x + w - tw - 11, y + 8, 0xFFFFC45E);
            } else if (row.type == RowType.COLOR) {
                int hintWidth = textRenderer.getWidth(row.detail);
                UiDraw.drawText(textRenderer, matrices, row.detail, x + w - 78 - hintWidth, y + 9, 0xFF9EACBE);
                UiDraw.roundedRect(matrices, x + w - 38, y + 5, 25, 18, row.color, 4);
                UiDraw.roundedRect(matrices, x + w - 65, y + 5, 19, 18, 0xFF101923, 4);
            }
        }
    }

    private String formatValue(SettingRow row) {
        if (row.format == Format.ANIMATION) return ModConfig.getAnimationPresetName(row.value, ModConfig.isEnglish());
        if (row.format == Format.DECIMAL_2) return String.format(java.util.Locale.ROOT, "%.2f %s", row.value / 100.0f, row.unit).trim();
        if (row.format == Format.REACH) return String.format(java.util.Locale.ROOT, "%.1f %s", row.value / 10.0f, row.unit);
        if (row.format == Format.RATIO) {
            switch (row.value) {
                case 1: return "4:3";
                case 2: return "16:10";
                default: return "16:9";
            }
        }
        return row.value + row.unit;
    }

    private String optionUnit() {
        boolean en = ModConfig.isEnglish();
        switch (module) {
            case ARMOR_HUD: case HELD_DURABILITY: case ARMOR_WARNINGS: case BETTER_PVP: return "%";
            case HIT_RANGE: case PROJECTILE_PREDICTOR: case CUSTOM_HITBOXES: case DANGER_INDICATOR:
                return en ? "blocks" : "блоков";
            case LOW_HEALTH_ALERT: case QUICK_HEAL_REMINDER: return "HP";
            case TOTEM_REFILL_ALERT: return en ? "items" : "шт.";
            default: return "";
        }
    }

    private Format optionFormat() {
        if (module == FeatureModule.HIT_RANGE) return Format.REACH;
        if (module == FeatureModule.ASPECT_RATIO) return Format.RATIO;
        return Format.INTEGER;
    }

    private int totalHeight(List<SettingRow> rows) {
        int result = 0;
        for (SettingRow row : rows) result += row.height;
        return result;
    }

    private void scissor(int top, int bottom) {
        double scale = client.getWindow().getScaleFactor();
        int sx = (int) Math.floor((panelX + 8) * scale);
        int sy = (int) Math.floor(client.getWindow().getFramebufferHeight() - bottom * scale);
        int sw = (int) Math.ceil((panelW - 16) * scale);
        int sh = (int) Math.ceil((bottom - top) * scale);
        RenderSystem.enableScissor(sx, sy, sw, sh);
    }

    private void updateSlider(SettingRow row, double mouseX) {
        int trackX = panelX + 14 + 12;
        int trackW = panelW - 28 - 24;
        double t = Math.max(0.0, Math.min(1.0, (mouseX - trackX) / Math.max(1.0, trackW)));
        int value = row.min + (int) Math.round(t * (row.max - row.min));
        setRowValue(row.id, value);
    }

    private void setRowValue(String id, int value) {
        switch (id) {
            case "option": ModConfig.updateOption(module, value); break;
            case "better_x": ModConfig.updateOption2(module, value); break;
            case "better_y": ModConfig.updateOption3(module, value); break;
            case "better_tilt": ModConfig.updateOption4(module, value); break;
            case "better_depth": ModConfig.updateOption5(module, value); break;
            case "better_scale": ModConfig.updateOption6(module, value); break;
            case "better_animation": ModConfig.updateOption7(module, value); break;
            case "better_curve": ModConfig.updateOption8(module, value); break;
            case "better_speed": ModConfig.updateOption9(module, value); break;
            case "better_swing_tilt": ModConfig.updateOption10(module, value); break;
            case "better_swing_shift": ModConfig.updateOption11(module, value); break;
            case "scale": ModConfig.updateScale(module, value); break;
            case "opacity": ModConfig.updateModuleOpacity(module, value); break;
            default: break;
        }
    }

    private int getRowValue(String id) {
        switch (id) {
            case "option": return ModConfig.getOption(module);
            case "better_x": return ModConfig.getOption2(module);
            case "better_y": return ModConfig.getOption3(module);
            case "better_tilt": return ModConfig.getOption4(module);
            case "better_depth": return ModConfig.getOption5(module);
            case "better_scale": return ModConfig.getOption6(module);
            case "better_animation": return ModConfig.getOption7(module);
            case "better_curve": return ModConfig.getOption8(module);
            case "better_speed": return ModConfig.getOption9(module);
            case "better_swing_tilt": return ModConfig.getOption10(module);
            case "better_swing_shift": return ModConfig.getOption11(module);
            case "scale": return ModConfig.getScale(module);
            case "opacity": return ModConfig.getModuleOpacity(module);
            default: return 0;
        }
    }

    private SettingRow rowAt(double mouseX, double mouseY) {
        List<SettingRow> rows = buildRows(ModConfig.isEnglish());
        int y = viewTop - (int) scroll;
        for (SettingRow row : rows) {
            if (mouseY >= y && mouseY < y + row.height && mouseX >= panelX + 14 && mouseX <= panelX + panelW - 14) return row;
            y += row.height;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (bindingModule != null) {
            if (button == 0) return true;
            ModKeybinds.setKey(bindingModule, InputUtil.Type.MOUSE.createFromCode(button));
            bindingModule = null;
            return true;
        }
        if (module == FeatureModule.AUTO_EZZ) {
            boolean insideText = mouseX >= panelX + 15 && mouseX <= panelX + panelW - 15
                    && mouseY >= panelY + 85 && mouseY <= panelY + 110;
            if (insideText) {
                autoEzzFocused = true;
                return true;
            }
            if (autoEzzFocused) {
                autoEzzFocused = false;
                ModConfig.setAutoEzzText(autoEzzText);
            }
        }
        SettingRow row = rowAt(mouseX, mouseY);
        if (row == null) return super.mouseClicked(mouseX, mouseY, button);
        if (row.type == RowType.SLIDER) {
            draggingSlider = row;
            updateSlider(row, mouseX);
            return true;
        }
        if (button != 0) return true;
        switch (row.id) {
            case "enabled": ModConfig.setEnabled(module, !ModConfig.isEnabled(module)); return true;
            case "bind": bindingModule = module; return true;
            case "better_template": ModConfig.applyBetterPvpTemplate(); return true;
            case "crosshair_paint": client.setScreen(new CrosshairPaintScreen()); return true;
            case "hud_editor": client.setScreen(new HudLayoutEditorScreen(module)); return true;
            case "reset_widget": ModConfig.resetWidget(module); return true;
            case "accent": ModConfig.cycleAccentColor(module); return true;
            case "background": ModConfig.cycleBackgroundColor(module); return true;
            case "text": ModConfig.cycleTextColor(module); return true;
            case "secondary": ModConfig.cycleSecondaryColor(module); return true;
            case "tertiary": ModConfig.cycleTertiaryColor(module); return true;
            default: return true;
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingSlider != null) {
            updateSlider(draggingSlider, mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingSlider != null) {
            draggingSlider = null;
            ModConfig.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        SettingRow row = rowAt(mouseX, mouseY);
        if (row != null && row.type == RowType.SLIDER) {
            setRowValue(row.id, Math.max(row.min, Math.min(row.max, getRowValue(row.id) + (amount > 0 ? 1 : -1))));
            ModConfig.save();
            return true;
        }
        if (mouseY >= viewTop && mouseY <= viewBottom) {
            scroll = Math.max(0, scroll - amount * 28.0);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (module == FeatureModule.AUTO_EZZ && autoEzzFocused && chr >= 32 && chr != 127 && autoEzzText.length() < 100) {
            autoEzzText += chr;
            ModConfig.updateAutoEzzText(autoEzzText);
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bindingModule != null) {
            if (keyCode == 256) bindingModule = null;
            else ModKeybinds.setKey(bindingModule, InputUtil.Type.KEYSYM.createFromCode(keyCode));
            bindingModule = null;
            return true;
        }
        if (module == FeatureModule.AUTO_EZZ && autoEzzFocused) {
            if (keyCode == 259) {
                if (!autoEzzText.isEmpty()) autoEzzText = autoEzzText.substring(0, autoEzzText.length() - 1);
                ModConfig.updateAutoEzzText(autoEzzText);
                return true;
            }
            if (keyCode == 257 || keyCode == 335) {
                autoEzzFocused = false;
                ModConfig.setAutoEzzText(autoEzzText);
                return true;
            }
            if (keyCode == 256) {
                autoEzzFocused = false;
                ModConfig.setAutoEzzText(autoEzzText);
            }
        }
        if (keyCode == 256) {
            ModConfig.save();
            client.setScreen(new StrengthMenuScreen());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private String keyName() {
        if (ModKeybinds.get(module) == null || ModKeybinds.get(module).isUnbound()) return ModConfig.isEnglish() ? "Unbound" : "Не назначена";
        return ModKeybinds.get(module).getBoundKeyLocalizedText().getString();
    }

    @Override public boolean isPauseScreen() { return false; }

    private enum RowType { TOGGLE, ACTION, SLIDER, COLOR }
    private enum Format { INTEGER, DECIMAL_2, REACH, RATIO, ANIMATION }

    private static final class SettingRow {
        final String id, label, unit, detail;
        final RowType type;
        final int min, max, value, color, height;
        final Format format;
        private SettingRow(String id, String label, RowType type, int min, int max, int value,
                           String unit, String detail, int color, Format format, int height) {
            this.id=id; this.label=label; this.type=type; this.min=min; this.max=max; this.value=value;
            this.unit=unit; this.detail=detail; this.color=color; this.format=format; this.height=height;
        }
        static SettingRow toggle(String id, String label, boolean value) { return new SettingRow(id,label,RowType.TOGGLE,0,1,value?1:0,"","",0,Format.INTEGER,30); }
        static SettingRow action(String id, String label, String detail) { return new SettingRow(id,label,RowType.ACTION,0,0,0,"",detail,0,Format.INTEGER,30); }
        static SettingRow slider(String id, String label, int min, int max, int value, String unit, Format format) { return new SettingRow(id,label,RowType.SLIDER,min,max,value,unit,"",0,format,42); }
        static SettingRow color(String id, String label, int color, String detail) { return new SettingRow(id,label,RowType.COLOR,0,0,0,"",detail,color,Format.INTEGER,30); }
    }
}
