package com.synxt.strengthpvp.ui;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;

public final class UiDraw {
    private static final Identifier SYNXT_FONT = new Identifier("strengthpvp", "synxt_ui");
    private UiDraw() {}

    /** Draws mod-owned labels using the bundled bitmap typeface, leaving vanilla/chat fonts untouched. */
    public static int drawText(TextRenderer renderer, MatrixStack matrices, String value, float x, float y, int color) {
        LiteralText text = new LiteralText(value);
        text.setStyle(Style.EMPTY.withFont(SYNXT_FONT));
        return renderer.draw(matrices, text, x, y, color);
    }

    public static void roundedRect(MatrixStack matrices, int x, int y, int width, int height, int color, int radius) {
        if (width <= 0 || height <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(width / 2, height / 2)));
        if (r == 0) {
            DrawableHelper.fill(matrices, x, y, x + width, y + height, color);
            return;
        }
        for (int row = 0; row < height; row++) {
            int inset = 0;
            if (row < r) {
                double dy = r - row - 0.5;
                inset = r - (int) Math.floor(Math.sqrt(Math.max(0.0, r * r - dy * dy)));
            } else if (row >= height - r) {
                double dy = row - (height - r) + 0.5;
                inset = r - (int) Math.floor(Math.sqrt(Math.max(0.0, r * r - dy * dy)));
            }
            DrawableHelper.fill(matrices, x + inset, y + row, x + width - inset, y + row + 1, color);
        }
    }

    public static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }
}
