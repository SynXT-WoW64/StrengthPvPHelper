package com.synxt.strengthpvp.ui;

import com.synxt.strengthpvp.config.ModConfig;
import com.synxt.strengthpvp.feature.FeatureModule;
import com.synxt.strengthpvp.feature.ModKeybinds;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class StrengthMenuScreen extends Screen {
    private static final int PANEL_WIDTH = 520;
    private static final int PANEL_HEIGHT = 320;
    private FeatureModule.Category selectedCategory = FeatureModule.Category.HUD;
    private FeatureModule bindingModule = null;

    public StrengthMenuScreen() {
        super(Text.of("StrengthPvPHelper"));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        int right = left + PANEL_WIDTH;
        int bottom = top + PANEL_HEIGHT;

        fill(matrices, left, top, right, bottom, 0xF0111722);
        fill(matrices, left, top, right, top + 38, 0xFF1A2636);
        fill(matrices, left, top + 38, left + 128, bottom, 0xFF131D2A);
        fill(matrices, left + 128, top + 38, left + 130, bottom, 0xFF29384B);

        textRenderer.draw(matrices, "STRENGTH", left + 16, top + 10, 0xFFFF5555);
        textRenderer.draw(matrices, "PVP HELPER  /  1.17.1", left + 105, top + 12, 0xFFB8C4D4);
        textRenderer.draw(matrices, "RIGHT SHIFT — меню", right - 135, top + 13, 0xFF8291A5);

        int categoryY = top + 56;
        for (FeatureModule.Category category : FeatureModule.Category.values()) {
            boolean selected = category == selectedCategory;
            int color = selected ? 0xFF293B52 : 0x00131D2A;
            fill(matrices, left + 9, categoryY - 4, left + 119, categoryY + 20, color);
            if (selected) fill(matrices, left + 9, categoryY - 4, left + 12, categoryY + 20, 0xFFFF5555);
            textRenderer.draw(matrices, category.title, left + 20, categoryY + 3, selected ? 0xFFFFFFFF : 0xFF9AA9BC);
            categoryY += 31;
        }

        int contentX = left + 144;
        int contentY = top + 54;
        textRenderer.draw(matrices, selectedCategory.title.toUpperCase(), contentX, contentY, 0xFFFF6666);
        textRenderer.draw(matrices, "Нажми на карточку для включения. Нижняя строка меняет клавишу.",
                contentX, contentY + 15, 0xFF8F9DB0);

        List<FeatureModule> modules = getModules(selectedCategory);
        int cardY = contentY + 38;
        for (int i = 0; i < modules.size(); i++) {
            FeatureModule module = modules.get(i);
            int col = i % 2;
            int row = i / 2;
            int x = contentX + col * 178;
            int y = cardY + row * 76;
            boolean enabled = ModConfig.isEnabled(module);
            fill(matrices, x, y, x + 166, y + 68, 0xFF1B2838);
            fill(matrices, x, y, x + 166, y + 2, enabled ? 0xFF53C98B : 0xFF435064);
            textRenderer.draw(matrices, module.title, x + 9, y + 8, 0xFFF0F4FA);
            String state = module.implemented ? (enabled ? "ВКЛ" : "ВЫКЛ") : "СЛЕДУЮЩИЙ ЭТАП";
            int stateColor = module.implemented ? (enabled ? 0xFF69D69C : 0xFF8995A5) : 0xFFFFC857;
            textRenderer.draw(matrices, state, x + 9, y + 24, stateColor);
            String bindText;
            if (!module.implemented) bindText = "Функция ещё в разработке";
            else bindText = bindingModule == module ? "Нажми клавишу..." : "Клавиша: " + ModKeybinds.get(module).getBoundKeyLocalizedText().getString();
            textRenderer.draw(matrices, bindText, x + 9, y + 46, bindingModule == module ? 0xFFFFC857 : 0xFF9AA9BC);
        }

        if (selectedCategory == FeatureModule.Category.HUD) {
            int settingsY = bottom - 31;
            fill(matrices, contentX, settingsY - 5, right - 16, settingsY + 17, 0xFF1B2838);
            textRenderer.draw(matrices, "Порог предупреждения брони:", contentX + 8, settingsY + 2, 0xFFCBD5E1);
            String threshold = ModConfig.getArmorWarningPercent() + "%   [нажми, чтобы изменить]";
            textRenderer.draw(matrices, threshold, contentX + 195, settingsY + 2, 0xFFFFC857);
        }

        textRenderer.draw(matrices, "Настройки сохраняются автоматически", left + 14, bottom - 15, 0xFF718198);
        super.render(matrices, mouseX, mouseY, delta);
    }

    private List<FeatureModule> getModules(FeatureModule.Category category) {
        List<FeatureModule> result = new ArrayList<>();
        for (FeatureModule module : FeatureModule.values()) {
            if (module.category == category) result.add(module);
        }
        return result;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        int right = left + PANEL_WIDTH;
        int bottom = top + PANEL_HEIGHT;

        if (bindingModule != null) {
            if (button >= 0) {
                InputUtil.Key key = InputUtil.Type.MOUSE.createFromCode(button);
                ModKeybinds.setKey(bindingModule, key);
                bindingModule = null;
                return true;
            }
        }

        int categoryY = top + 56;
        for (FeatureModule.Category category : FeatureModule.Category.values()) {
            if (inside(mouseX, mouseY, left + 9, categoryY - 4, left + 119, categoryY + 20)) {
                selectedCategory = category;
                bindingModule = null;
                return true;
            }
            categoryY += 31;
        }

        int contentX = left + 144;
        int cardY = top + 54 + 38;
        List<FeatureModule> modules = getModules(selectedCategory);
        for (int i = 0; i < modules.size(); i++) {
            FeatureModule module = modules.get(i);
            int col = i % 2;
            int row = i / 2;
            int x = contentX + col * 178;
            int y = cardY + row * 76;
            if (inside(mouseX, mouseY, x, y, x + 166, y + 68)) {
                if (!module.implemented) return true;
                if (mouseY >= y + 40) {
                    bindingModule = module;
                } else {
                    ModConfig.setEnabled(module, !ModConfig.isEnabled(module));
                }
                return true;
            }
        }

        if (selectedCategory == FeatureModule.Category.HUD) {
            int settingsY = bottom - 31;
            if (inside(mouseX, mouseY, contentX, settingsY - 5, right - 16, settingsY + 17)) {
                int next = ModConfig.getArmorWarningPercent() + 5;
                if (next > 30) next = 10;
                ModConfig.setArmorWarningPercent(next);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bindingModule != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                bindingModule = null;
            } else {
                ModKeybinds.setKey(bindingModule, InputUtil.Type.KEYSYM.createFromCode(keyCode));
                bindingModule = null;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            client.setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean inside(double mouseX, double mouseY, int x1, int y1, int x2, int y2) {
        return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
