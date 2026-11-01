package com.synxt.strengthpvp.ui;

import com.synxt.strengthpvp.config.ModConfig;
import com.synxt.strengthpvp.feature.FeatureModule;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Pixel-art editor for the 15x15 crosshair texture. */
public final class CrosshairPaintScreen extends Screen {
    private static final int CELL = 17;
    private static final int GRID = 15;
    private static final int[] PALETTE = ModConfig.CROSSHAIR_PALETTE;
    private final List<String> undo = new ArrayList<>();
    private final List<String> redo = new ArrayList<>();
    private int panelX, panelY, panelW, panelH, gridX, gridY;
    private int selectedColor;
    private int brushSize = 1;
    private int lastPaintX = -1, lastPaintY = -1;
    private int paintButton;
    private boolean eraser;
    private boolean fillMode;
    private boolean painting;

    public CrosshairPaintScreen() {
        super(Text.of("SynXT Pixel Crosshair Studio"));
        selectedColor = ModConfig.getCrosshairColor();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        panelW = Math.min(620, width - 20);
        panelH = Math.min(430, height - 20);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        gridX = panelX + 30;
        gridY = panelY + 68;

        UiDraw.roundedRect(matrices, panelX, panelY, panelW, panelH, 0xF0101722, 10);
        UiDraw.roundedRect(matrices, panelX, panelY, panelW, 38, 0xFF1B293B, 10);
        UiDraw.roundedRect(matrices, panelX, panelY + 26, panelW, 12, 0xFF1B293B, 0);
        UiDraw.drawText(textRenderer, matrices, "SYNXT  /  PIXEL CROSSHAIR STUDIO", panelX + 16, panelY + 14, 0xFFFF6874);
        UiDraw.drawText(textRenderer, matrices,
                ModConfig.isEnglish() ? "Left: paint · Right: erase · Middle: pick color · Wheel: brush size"
                        : "ЛКМ: рисовать · ПКМ: стирать · СКМ: пипетка · Колесо: размер кисти",
                panelX + 30, panelY + 48, 0xFFAAB7C7);

        String pixels = ModConfig.getCrosshairPixels();
        UiDraw.roundedRect(matrices, gridX - 7, gridY - 7, GRID * CELL + 14, GRID * CELL + 14, 0xFF0C121B, 6);
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                int index = y * GRID + x;
                int colorIndex = index < pixels.length() ? pixels.charAt(index) - '0' : 0;
                int x1 = gridX + x * CELL;
                int y1 = gridY + y * CELL;
                DrawableHelper.fill(matrices, x1, y1, x1 + CELL - 1, y1 + CELL - 1,
                        ((x + y) & 1) == 0 ? 0xFF202B39 : 0xFF263344);
                if (colorIndex > 0) {
                    DrawableHelper.fill(matrices, x1 + 2, y1 + 2, x1 + CELL - 3, y1 + CELL - 3,
                            ModConfig.getCrosshairPaletteColor(colorIndex));
                }
                if (mouseX >= x1 && mouseX < x1 + CELL && mouseY >= y1 && mouseY < y1 + CELL) {
                    UiDraw.roundedRect(matrices, x1 + 1, y1 + 1, CELL - 2, CELL - 2, 0x66FFFFFF, 1);
                }
            }
        }

        int toolX = gridX + GRID * CELL + 42;
        UiDraw.drawText(textRenderer, matrices, ModConfig.isEnglish() ? "PALETTE" : "ПАЛИТРА", toolX, gridY + 3, 0xFFEAF0F7);
        for (int i = 0; i < PALETTE.length; i++) {
            int x = toolX + (i % 4) * 38;
            int y = gridY + 24 + (i / 4) * 38;
            boolean selected = !eraser && selectedColor == i + 1;
            UiDraw.roundedRect(matrices, x - 3, y - 3, 32, 32, selected ? 0xFFFF6874 : 0xFF3A4B60, 5);
            UiDraw.roundedRect(matrices, x, y, 26, 26, PALETTE[i], 3);
        }

        int toolsY = gridY + 108;
        button(matrices, toolX, toolsY, 142, 23, ModConfig.isEnglish() ? "Undo  [Ctrl+Z]" : "Отменить  [Ctrl+Z]", 0xFF30435A, undo.isEmpty());
        button(matrices, toolX, toolsY + 27, 142, 23, ModConfig.isEnglish() ? "Redo  [Ctrl+Y]" : "Повторить  [Ctrl+Y]", 0xFF30435A, redo.isEmpty());
        button(matrices, toolX, toolsY + 54, 142, 23,
                eraser ? (ModConfig.isEnglish() ? "Eraser: ON" : "Ластик: ВКЛ") : (ModConfig.isEnglish() ? "Eraser" : "Ластик"),
                eraser ? 0xFF71404A : 0xFF30435A, false);
        button(matrices, toolX, toolsY + 81, 142, 23,
                fillMode ? (ModConfig.isEnglish() ? "Bucket: ON" : "Заливка: ВКЛ") : (ModConfig.isEnglish() ? "Fill area" : "Залить область"),
                fillMode ? 0xFF536C91 : 0xFF30435A, false);
        button(matrices, toolX, toolsY + 108, 142, 23, ModConfig.isEnglish() ? "Clear canvas" : "Очистить холст", 0xFF49343B, false);
        button(matrices, toolX, toolsY + 135, 142, 23, ModConfig.isEnglish() ? "Reset template" : "Вернуть шаблон", 0xFF30435A, false);
        button(matrices, toolX, toolsY + 168, 142, 23, ModConfig.isEnglish() ? "Save & back" : "Сохранить и назад", 0xFF285342, false);
        UiDraw.drawText(textRenderer, matrices,
                (ModConfig.isEnglish() ? "Brush: " : "Кисть: ") + brushSize + " × " + brushSize,
                toolX, toolsY + 202, 0xFF9EADBF);
        UiDraw.drawText(textRenderer, matrices,
                (ModConfig.isEnglish() ? "In-game pixel size: " : "Размер пикселя в игре: ")
                        + ModConfig.getScale(FeatureModule.CROSSHAIR_CUSTOM) + "%",
                toolX, toolsY + 217, 0xFF9EADBF);
        super.render(matrices, mouseX, mouseY, delta);
    }

    private void button(MatrixStack matrices, int x, int y, int w, int h, String label, int color, boolean disabled) {
        UiDraw.roundedRect(matrices, x, y, w, h, disabled ? 0xFF202A36 : color, 5);
        int tx = x + (w - textRenderer.getWidth(label)) / 2;
        UiDraw.drawText(textRenderer, matrices, label, tx, y + 7, disabled ? 0xFF657286 : 0xFFF0F4F9);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int toolX = gridX + GRID * CELL + 42;
        int toolsY = gridY + 108;
        if (mouseX >= gridX && mouseY >= gridY && mouseX < gridX + GRID * CELL && mouseY < gridY + GRID * CELL) {
            int x = (int) ((mouseX - gridX) / CELL);
            int y = (int) ((mouseY - gridY) / CELL);
            if (button == 2) {
                pickColorAt(x, y);
                return true;
            }
            if (fillMode) {
                pushUndo();
                fillAt(x, y, button);
                return true;
            }
            pushUndo();
            painting = true;
            paintButton = button;
            lastPaintX = x;
            lastPaintY = y;
            paintPixel(x, y, button);
            return true;
        }
        for (int i = 0; i < PALETTE.length; i++) {
            int x = toolX + (i % 4) * 38;
            int y = gridY + 24 + (i / 4) * 38;
            if (inside(mouseX, mouseY, x - 3, y - 3, 32, 32)) {
                selectedColor = i + 1;
                eraser = false;
                ModConfig.setCrosshairColor(selectedColor);
                return true;
            }
        }
        if (inside(mouseX, mouseY, toolX, toolsY, 142, 23)) { undo(); return true; }
        if (inside(mouseX, mouseY, toolX, toolsY + 27, 142, 23)) { redo(); return true; }
        if (inside(mouseX, mouseY, toolX, toolsY + 54, 142, 23)) { eraser = !eraser; fillMode = false; return true; }
        if (inside(mouseX, mouseY, toolX, toolsY + 81, 142, 23)) { fillMode = !fillMode; eraser = false; return true; }
        if (inside(mouseX, mouseY, toolX, toolsY + 108, 142, 23)) {
            pushUndo(); ModConfig.clearCrosshair(); ModConfig.save(); return true;
        }
        if (inside(mouseX, mouseY, toolX, toolsY + 135, 142, 23)) {
            pushUndo(); ModConfig.resetCrosshair(); selectedColor = 1; eraser = false; fillMode = false; return true;
        }
        if (inside(mouseX, mouseY, toolX, toolsY + 168, 142, 23)) {
            ModConfig.save(); client.setScreen(new ModuleSettingsScreen(FeatureModule.CROSSHAIR_CUSTOM)); return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!painting) return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        if (inside(mouseX, mouseY, gridX, gridY, GRID * CELL, GRID * CELL)) {
            int x = (int) ((mouseX - gridX) / CELL);
            int y = (int) ((mouseY - gridY) / CELL);
            drawLine(lastPaintX, lastPaintY, x, y, paintButton);
            lastPaintX = x;
            lastPaintY = y;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (painting) {
            painting = false;
            lastPaintX = lastPaintY = -1;
            ModConfig.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (inside(mouseX, mouseY, gridX, gridY, GRID * CELL, GRID * CELL)) {
            brushSize = Math.max(1, Math.min(3, brushSize + (amount > 0 ? 1 : -1)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    private void drawLine(int x0, int y0, int x1, int y1, int button) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int error = dx + dy;
        while (true) {
            paintPixel(x0, y0, button);
            if (x0 == x1 && y0 == y1) break;
            int twice = 2 * error;
            if (twice >= dy) { error += dy; x0 += sx; }
            if (twice <= dx) { error += dx; y0 += sy; }
        }
    }

    private void paintPixel(int x, int y, int button) {
        int value = button == 1 || eraser ? 0 : selectedColor;
        int start = -(brushSize - 1) / 2;
        int end = brushSize / 2;
        for (int oy = start; oy <= end; oy++) {
            for (int ox = start; ox <= end; ox++) {
                ModConfig.paintCrosshairPixel(x + ox, y + oy, value);
            }
        }
    }

    private void pickColorAt(int x, int y) {
        String pixels = ModConfig.getCrosshairPixels();
        int index = y * GRID + x;
        int value = index >= 0 && index < pixels.length() ? pixels.charAt(index) - '0' : 0;
        if (value == 0) {
            eraser = true;
        } else {
            selectedColor = value;
            eraser = false;
            ModConfig.setCrosshairColor(selectedColor);
        }
        fillMode = false;
    }

    private void fillAt(int x, int y, int button) {
        String source = ModConfig.getCrosshairPixels();
        char[] pixels = source.toCharArray();
        int start = y * GRID + x;
        char target = pixels[start];
        char replacement = (char) ('0' + (button == 1 || eraser ? 0 : selectedColor));
        if (target == replacement) return;
        int[] queue = new int[GRID * GRID];
        boolean[] visited = new boolean[GRID * GRID];
        int head = 0, tail = 0;
        queue[tail++] = start;
        visited[start] = true;
        while (head < tail) {
            int index = queue[head++];
            pixels[index] = replacement;
            int px = index % GRID;
            int py = index / GRID;
            if (px > 0) tail = enqueue(index - 1, target, pixels, visited, queue, tail);
            if (px < GRID - 1) tail = enqueue(index + 1, target, pixels, visited, queue, tail);
            if (py > 0) tail = enqueue(index - GRID, target, pixels, visited, queue, tail);
            if (py < GRID - 1) tail = enqueue(index + GRID, target, pixels, visited, queue, tail);
        }
        ModConfig.setCrosshairPixels(new String(pixels));
    }

    private int enqueue(int index, char target, char[] pixels, boolean[] visited, int[] queue, int tail) {
        if (!visited[index] && pixels[index] == target) {
            visited[index] = true;
            queue[tail++] = index;
        }
        return tail;
    }

    private void pushUndo() {
        String current = ModConfig.getCrosshairPixels();
        if (undo.isEmpty() || !undo.get(undo.size() - 1).equals(current)) {
            undo.add(current);
            if (undo.size() > 64) undo.remove(0);
        }
        redo.clear();
    }

    private void undo() {
        if (undo.isEmpty()) return;
        redo.add(ModConfig.getCrosshairPixels());
        ModConfig.setCrosshairPixels(undo.remove(undo.size() - 1));
    }

    private void redo() {
        if (redo.isEmpty()) return;
        undo.add(ModConfig.getCrosshairPixels());
        ModConfig.setCrosshairPixels(redo.remove(redo.size() - 1));
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx <= x + w && my <= y + h;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((modifiers & 2) != 0 && (keyCode == 90 || keyCode == 89)) {
            if (keyCode == 90) undo(); else redo();
            return true;
        }
        if (keyCode == 256) {
            ModConfig.save();
            client.setScreen(new ModuleSettingsScreen(FeatureModule.CROSSHAIR_CUSTOM));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
