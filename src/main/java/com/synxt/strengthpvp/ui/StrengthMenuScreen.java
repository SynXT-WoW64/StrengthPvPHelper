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

public class StrengthMenuScreen extends Screen {
    private static final int HEADER_HEIGHT = 34;
    private FeatureModule.Category selectedCategory = FeatureModule.Category.HUD;
    private FeatureModule bindingModule;
    private double scroll;

    public StrengthMenuScreen() {
        super(Text.of("StrengthPvPHelper"));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        Layout l = layout();
        int alpha = Math.round(255.0f * ModConfig.getOpacityPercent() / 100.0f);

        UiDraw.roundedRect(matrices, l.x, l.y, l.w, l.h, UiDraw.withAlpha(0x101722, alpha), 9);
        UiDraw.roundedRect(matrices, l.x, l.y, l.w, HEADER_HEIGHT, UiDraw.withAlpha(0x1B293B, Math.min(255, alpha + 12)), 9);
        // Square off the lower half of the header while keeping rounded top corners.
        UiDraw.roundedRect(matrices, l.x, l.y + 22, l.w, 12, UiDraw.withAlpha(0x1B293B, Math.min(255, alpha + 12)), 0);
        UiDraw.roundedRect(matrices, l.x + 8, l.y + HEADER_HEIGHT + 9, l.navW, l.h - HEADER_HEIGHT - 18,
                UiDraw.withAlpha(0x121C2A, alpha), 6);

        UiDraw.drawText(textRenderer, matrices, l.w < 225 ? "S" : "STRENGTH", l.x + 14, l.y + 11, 0xFFFF626B);
        if (l.w >= 470) {
            UiDraw.drawText(textRenderer, matrices, "PVP HELPER  /  1.17.1  ·  SYNXT", l.x + 100, l.y + 11, 0xFFD2DAE5);
        }
        String language = ModConfig.isEnglish() ? "EN" : "RU";
        button(matrices, l.langX, l.y + 7, 42, 20, language, 0xFF2B3B50);
        button(matrices, l.opacityX, l.y + 7, 58, 20, "ALPHA " + (100 - ModConfig.getOpacityPercent()) + "%", 0xFF2B3B50);
        if (l.w >= 560) button(matrices, l.editorX, l.y + 7, 66, 20, "HUD EDIT", 0xFF34465D);

        renderNavigation(matrices, l);
        renderModules(matrices, l, alpha);
        String footer = bindingModule != null
                ? text("Нажми клавишу или Esc для отмены", "Press a key or Esc to cancel")
                : text("Автосохранение · колесо мыши — прокрутка", "Auto-save · mouse wheel to scroll");
        UiDraw.drawText(textRenderer, matrices, trim(footer, l.w - 28), l.x + 14, l.y + l.h - 17, 0xFF8493A7);
        super.render(matrices, mouseX, mouseY, delta);
    }

    private void renderNavigation(MatrixStack matrices, Layout l) {
        int y = l.y + HEADER_HEIGHT + 20;
        for (FeatureModule.Category category : FeatureModule.Category.values()) {
            boolean selected = category == selectedCategory;
            int rowHeight = l.navRowHeight;
            if (selected) {
                UiDraw.roundedRect(matrices, l.x + 13, y - 4, l.navW - 10, rowHeight, 0xFF2A3C53, 5);
                UiDraw.roundedRect(matrices, l.x + 13, y - 1, 3, rowHeight - 6, 0xFFFF626B, 2);
            }
            String label = trim(category.title(ModConfig.isEnglish()), l.navW - 36);
            UiDraw.drawText(textRenderer, matrices, label, l.x + 25, y + 4, selected ? 0xFFF6F8FB : 0xFF9CAABD);
            y += rowHeight + 5;
        }
    }

    private void renderModules(MatrixStack matrices, Layout l, int alpha) {
        boolean english = ModConfig.isEnglish();
        List<FeatureModule> modules = modulesFor(selectedCategory);
        int columns = l.contentW >= 410 ? 2 : 1;
        int cardGap = 9;
        int cardWidth = (l.contentW - (columns - 1) * cardGap) / columns;
        int cardHeight = l.cardHeight;
        int pitch = cardHeight + 8;
        int rows = (modules.size() + columns - 1) / columns;
        double maxScroll = Math.max(0, rows * pitch - 8 - l.viewH);
        scroll = MathHelper.clamp(scroll, 0.0, maxScroll);

        UiDraw.drawText(textRenderer, matrices, selectedCategory.title(english).toUpperCase(), l.contentX, l.y + HEADER_HEIGHT + 13, 0xFFFF6B73);
        UiDraw.drawText(textRenderer, matrices, text("ЛКМ: вкл/выкл · ПКМ: настройки · строка клавиши: бинды",
                "LMB: toggle · RMB: settings · key row: keybind"), l.contentX, l.y + HEADER_HEIGHT + 27, 0xFF8E9DB1);

        int alphaCard = Math.min(255, alpha + 8);
        scissor(l.viewX, l.viewY, l.contentW, l.viewH);
        for (int i = 0; i < modules.size(); i++) {
            FeatureModule module = modules.get(i);
            int col = i % columns;
            int row = i / columns;
            int x = l.contentX + col * (cardWidth + cardGap);
            int y = l.viewY + row * pitch - (int) scroll;
            if (y + cardHeight < l.viewY || y > l.viewY + l.viewH) continue;

            boolean enabled = ModConfig.isEnabled(module);
            int background = enabled ? 0xFF1D2D3E : 0xFF1A2635;
            UiDraw.roundedRect(matrices, x, y, cardWidth, cardHeight,
                    UiDraw.withAlpha(background, alphaCard), 6);
            UiDraw.roundedRect(matrices, x + 7, y + 5, cardWidth - 14, 2,
                    enabled ? 0xFF58D69B : 0xFF46566B, 1);

            int titleColor = 0xFFF0F4F9;
            UiDraw.drawText(textRenderer, matrices, trim(module.title(english), cardWidth - 19), x + 9, y + 11, titleColor);
            String desc = trim(module.description(english), cardWidth - 18);
            UiDraw.drawText(textRenderer, matrices, desc, x + 9, y + 25, 0xFF9EACBE);
            String state = enabled ? text("ВКЛ", "ON") : text("ВЫКЛ", "OFF");
            UiDraw.drawText(textRenderer, matrices, state, x + 9, y + 39, enabled ? 0xFF70DCA5 : 0xFF8996A7);

            if (module.hasOption()) {
                String option = module.optionLabel(english) + ": " + ModConfig.getOption(module) + (module == FeatureModule.ARMOR_HUD ? "%" : "");
                int right = x + cardWidth - 9 - textRenderer.getWidth(option);
                UiDraw.drawText(textRenderer, matrices, trim(option, cardWidth - 18), Math.max(x + 65, right), y + 39, 0xFFFFC45E);
            }

            String bind;
            if (bindingModule == module) {
                bind = text("Нажми клавишу…", "Press a key…");
            } else {
                bind = text("Клавиша: ", "Key: ") + keyName(module);
            }
            UiDraw.drawText(textRenderer, matrices, trim(bind, cardWidth - 18), x + 9, y + 55, bindingModule == module ? 0xFFFFC45E : 0xFF9BAABD);
        }
        RenderSystem.disableScissor();

        if (rows * pitch > l.viewH) {
            int trackX = l.contentX + l.contentW - 3;
            UiDraw.roundedRect(matrices, trackX, l.viewY, 2, l.viewH, 0xFF39475A, 1);
            int thumbHeight = Math.max(18, (int) (l.viewH * (l.viewH / (double) (rows * pitch))));
            int thumbY = l.viewY + (int) ((l.viewH - thumbHeight) * (scroll / Math.max(1.0, maxScroll)));
            UiDraw.roundedRect(matrices, trackX, thumbY, 2, thumbHeight, 0xFF9EADBF, 1);
        }
    }

    private void button(MatrixStack matrices, int x, int y, int width, int height, String label, int color) {
        UiDraw.roundedRect(matrices, x, y, width, height, color, 5);
        int tx = x + (width - textRenderer.getWidth(label)) / 2;
        UiDraw.drawText(textRenderer, matrices, label, tx, y + (height - 8) / 2, 0xFFE0E7F0);
    }

    private void scissor(int x, int y, int width, int height) {
        double scale = client.getWindow().getScaleFactor();
        int sx = (int) Math.floor(x * scale);
        int sy = (int) Math.floor(client.getWindow().getFramebufferHeight() - (y + height) * scale);
        int sw = (int) Math.ceil(width * scale);
        int sh = (int) Math.ceil(height * scale);
        RenderSystem.enableScissor(sx, sy, sw, sh);
    }

    private List<FeatureModule> modulesFor(FeatureModule.Category category) {
        List<FeatureModule> result = new ArrayList<>();
        for (FeatureModule module : FeatureModule.values()) if (module.category == category) result.add(module);
        return result;
    }

    private String keyName(FeatureModule module) {
        if (ModKeybinds.get(module) == null || ModKeybinds.get(module).isUnbound()) return text("Не назначена", "Unbound");
        return ModKeybinds.get(module).getBoundKeyLocalizedText().getString();
    }

    private String trim(String value, int maxWidth) {
        if (maxWidth <= 0) return "";
        if (textRenderer.getWidth(value) <= maxWidth) return value;
        String ellipsis = "…";
        String candidate = value;
        while (!candidate.isEmpty() && textRenderer.getWidth(candidate + ellipsis) > maxWidth) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        return candidate + ellipsis;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Layout l = layout();
        if (bindingModule != null) {
            if (button >= 0) {
                ModKeybinds.setKey(bindingModule, InputUtil.Type.MOUSE.createFromCode(button));
                bindingModule = null;
                return true;
            }
        }
        if (inside(mouseX, mouseY, l.langX, l.y + 7, l.langX + 42, l.y + 27)) {
            ModConfig.toggleLanguage();
            scroll = 0;
            return true;
        }
        if (inside(mouseX, mouseY, l.opacityX, l.y + 7, l.opacityX + 58, l.y + 27)) {
            ModConfig.cycleOpacity();
            return true;
        }
        if (l.w >= 560 && inside(mouseX, mouseY, l.editorX, l.y + 7, l.editorX + 66, l.y + 27)) {
            client.setScreen(new HudLayoutEditorScreen());
            return true;
        }

        int y = l.y + HEADER_HEIGHT + 20;
        for (FeatureModule.Category category : FeatureModule.Category.values()) {
            if (inside(mouseX, mouseY, l.x + 13, y - 4, l.x + 13 + l.navW - 10, y - 4 + l.navRowHeight)) {
                selectedCategory = category;
                scroll = 0;
                bindingModule = null;
                return true;
            }
            y += l.navRowHeight + 5;
        }

        if (inside(mouseX, mouseY, l.viewX, l.viewY, l.viewX + l.contentW, l.viewY + l.viewH)) {
            List<FeatureModule> modules = modulesFor(selectedCategory);
            int columns = l.contentW >= 410 ? 2 : 1;
            int cardGap = 9;
            int cardWidth = (l.contentW - (columns - 1) * cardGap) / columns;
            int pitch = l.cardHeight + 8;
            for (int i = 0; i < modules.size(); i++) {
                FeatureModule module = modules.get(i);
                int col = i % columns;
                int row = i / columns;
                int x = l.contentX + col * (cardWidth + cardGap);
                int cy = l.viewY + row * pitch - (int) scroll;
                if (!inside(mouseX, mouseY, x, cy, x + cardWidth, cy + l.cardHeight)) continue;
                if (button == 1) {
                    client.setScreen(new ModuleSettingsScreen(module));
                    return true;
                }
                if (button != 0) return true;
                int relativeY = (int) mouseY - cy;
                if (relativeY >= 53) {
                    bindingModule = module;
                } else if (module.hasOption() && relativeY >= 36) {
                    int value = ModConfig.getOption(module) + module.optionStep;
                    if (value > module.maxOption) value = module.minOption;
                    ModConfig.setOption(module, value);
                } else {
                    ModConfig.setEnabled(module, !ModConfig.isEnabled(module));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        Layout l = layout();
        if (inside(mouseX, mouseY, l.viewX, l.viewY, l.viewX + l.contentW, l.viewY + l.viewH)) {
            scroll -= amount * 28.0;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bindingModule != null) {
            if (keyCode == 256) {
                bindingModule = null;
            } else {
                ModKeybinds.setKey(bindingModule, InputUtil.Type.KEYSYM.createFromCode(keyCode));
                bindingModule = null;
            }
            return true;
        }
        if (keyCode == 256) {
            client.setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean inside(double mx, double my, int x1, int y1, int x2, int y2) {
        return mx >= x1 && mx <= x2 && my >= y1 && my <= y2;
    }

    private String text(String ru, String en) {
        return ModConfig.isEnglish() ? en : ru;
    }

    private Layout layout() {
        // Keep the panel inside windowed clients, including small 4:3 game windows.
        int panelWidth = Math.min(760, Math.max(180, width - 16));
        int panelHeight = Math.min(500, Math.max(180, height - 16));
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;
        int nav = Math.min(132, Math.max(72, panelWidth / 5));
        int contentX = x + nav + 22;
        int contentW = Math.max(1, x + panelWidth - 15 - contentX);
        int viewY = y + HEADER_HEIGHT + 53;
        int viewBottom = y + panelHeight - 28;
        int viewH = Math.max(42, viewBottom - viewY);
        int navRow = Math.max(18, Math.min(26, (panelHeight - 76) / 5));
        int cardHeight = Math.max(62, Math.min(72, (panelHeight - 90) / 4));
        return new Layout(x, y, panelWidth, panelHeight, nav, contentX, contentW, viewX(contentX), viewY, viewH,
                cardHeight, navRow, x + panelWidth - 122, x + panelWidth - 68, x + panelWidth - 195);
    }

    private int viewX(int contentX) { return contentX; }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class Layout {
        final int x, y, w, h, navW, contentX, contentW, viewX, viewY, viewH, cardHeight, navRowHeight, langX, opacityX, editorX;
        Layout(int x, int y, int w, int h, int navW, int contentX, int contentW, int viewX, int viewY, int viewH,
               int cardHeight, int navRowHeight, int langX, int opacityX, int editorX) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.navW = navW; this.contentX = contentX;
            this.contentW = contentW; this.viewX = viewX; this.viewY = viewY; this.viewH = viewH;
            this.cardHeight = cardHeight; this.navRowHeight = navRowHeight; this.langX = langX; this.opacityX = opacityX; this.editorX = editorX;
        }
    }
}
