package com.example.client.gui;

import com.example.client.module.Module;
import com.example.client.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.List;

public class ClickGuiScreen extends Screen {

    // ===== Размеры окна =====
    private static final int W = 380;
    private static final int H = 240;
    private static final int HEADER_H = 28;
    private static final int SIDEBAR_W = 90;
    private static final int ROW_H = 22;
    private static final int ROW_GAP = 4;
    private static final int COL_GAP = 6;

    private float guiX = 8;
    private float guiY = 20;

    private int selectedCategory = 0;

    private static final String[] CATS = {
            "Combat", "Movement", "Render", "Misc"
    };

    private boolean dragging = false;
    private double dragOffX, dragOffY;

    private long openTime;
    private float openAnim = 0f;

    public ClickGuiScreen() {
        super(Text.literal("ClickGUI"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        openTime = System.currentTimeMillis();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // анимация открытия
        long elapsed = System.currentTimeMillis() - openTime;
        float t = Math.min(1f, elapsed / 200f);
        openAnim = 1f - (1f - t) * (1f - t);

        ctx.fill(0, 0, width, height, ((int) (0x50 * openAnim)) << 24);

        // анимация тумблеров
        for (Module m : ModuleManager.modules) {
            float target = m.enabled ? 1f : 0f;
            m.anim += (target - m.anim) * 0.25f;
        }

        // масштаб при открытии
        float scale = 0.95f + 0.05f * openAnim;
        int cx = (int) guiX + W / 2;
        int cy = (int) guiY + H / 2;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(cx, cy, 0);
        ctx.getMatrices().scale(scale, scale, 1);
        ctx.getMatrices().translate(-cx, -cy, 0);

        drawWindow(ctx, mouseX, mouseY);

        ctx.getMatrices().pop();
    }

    private void drawWindow(DrawContext ctx, int mouseX, int mouseY) {
        int x = (int) guiX;
        int y = (int) guiY;

        // тень
        ctx.fill(x + 4, y + 5, x + W + 4, y + H + 5, 0x60000000);

        // основное окно
        ctx.fill(x, y, x + W, y + H, 0xFF14141A);

        // шапка
        ctx.fill(x, y, x + W, y + HEADER_H, 0xFF101015);

        // разделитель под шапкой
        ctx.fill(x, y + HEADER_H - 1, x + W, y + HEADER_H, 0xFF22222A);

        // радужная полоска сверху (тонкая)
        for (int i = 0; i < W; i++) {
            float hue = ((System.currentTimeMillis() % 4000L) / 4000f + i * 0.004f) % 1f;
            int col = Color.HSBtoRGB(hue, 0.65f, 1f) | 0xFF000000;
            ctx.fill(x + i, y, x + i + 1, y + 2, col);
        }

        // логотип (маленькая звёздочка)
        drawStar(ctx, x + 10, y + 9, 0xFFFFFFFF);

        // заголовок
        ctx.drawTextWithShadow(textRenderer, Text.literal("Client"), x + 26, y + 10, 0xFF6A6A72);
        ctx.drawTextWithShadow(textRenderer, Text.literal(" / "), x + 58, y + 10, 0xFF3A3A42);
        ctx.drawTextWithShadow(textRenderer, Text.literal(CATS[selectedCategory]),
                x + 72, y + 10, 0xFFFFFFFF);

        // плейсхолдер поиска справа
        int sw = 100;
        int sx = x + W - sw - 8;
        ctx.fill(sx, y + 7, sx + sw, y + 22, 0xFF1F1F26);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Search..."),
                sx + 6, y + 11, 0xFF56565E);

        // ===== Сайдбар =====
        ctx.fill(x, y + HEADER_H, x + SIDEBAR_W, y + H, 0xFF101015);
        ctx.fill(x + SIDEBAR_W, y + HEADER_H, x + SIDEBAR_W + 1, y + H, 0xFF22222A);

        for (int i = 0; i < CATS.length; i++) {
            int cy2 = y + HEADER_H + 12 + i * 28;
            boolean hover = mouseX >= x + 4 && mouseX <= x + SIDEBAR_W - 4
                    && mouseY >= cy2 && mouseY <= cy2 + 22;

            if (i == selectedCategory) {
                ctx.fill(x + 4, cy2, x + SIDEBAR_W - 4, cy2 + 22, 0xFF22222C);
                ctx.fill(x + 4, cy2, x + 6, cy2 + 22, rainbow(0));
            } else if (hover) {
                ctx.fill(x + 4, cy2, x + SIDEBAR_W - 4, cy2 + 22, 0xFF1A1A20);
            }

            // маленькая точка-иконка
            int dotColor = i == selectedCategory ? 0xFFFFFFFF : 0xFF5A5A62;
            ctx.fill(x + 14, cy2 + 9, x + 18, cy2 + 13, dotColor);

            int textColor = i == selectedCategory ? 0xFFFFFFFF : 0xFF9A9AA2;
            ctx.drawTextWithShadow(textRenderer, Text.literal(CATS[i]),
                    x + 26, cy2 + 7, textColor);
        }

        // ===== Сетка модулей (2 колонки) =====
        int contentX = x + SIDEBAR_W + 8;
        int contentY = y + HEADER_H + 8;
        int contentW = W - SIDEBAR_W - 16;
        int colW = (contentW - COL_GAP) / 2;

        List<Module> mods = ModuleManager.byCategory(CATS[selectedCategory]);

        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int col = i % 2;
            int row = i / 2;

            int cardX = contentX + col * (colW + COL_GAP);
            int cardY = contentY + row * (ROW_H + ROW_GAP);

            if (cardY + ROW_H > y + H - 4) break;

            boolean hover = mouseX >= cardX && mouseX <= cardX + colW
                    && mouseY >= cardY && mouseY <= cardY + ROW_H;

            int bg = hover ? 0xFF23232E : 0xFF1E1E26;
            ctx.fill(cardX, cardY, cardX + colW, cardY + ROW_H, bg);

            // текст
            int textColor = m.enabled ? 0xFFFFFFFF : 0xFF9A9AA2;
            ctx.drawTextWithShadow(textRenderer, Text.literal(m.name),
                    cardX + 8, cardY + 7, textColor);

            // тумблер
            int tsw = 22;
            int tsh = 12;
            int tx = cardX + colW - tsw - 8;
            int ty = cardY + (ROW_H - tsh) / 2;

            // фон тумблера
            ctx.fill(tx, ty, tx + tsw, ty + tsh, 0xFF33333E);

            // заливка (по анимации)
            if (m.anim > 0.01f) {
                int fillW = Math.max(1, (int) (tsw * m.anim));
                int col2 = m.enabled ? rainbow(0) : 0xFF44444E;
                ctx.fill(tx, ty, tx + fillW, ty + tsh, col2);
            }

            // кружок
            int knob = tsh - 2;
            int kx = tx + 1 + (int) ((tsw - knob - 2) * m.anim);
            ctx.fill(kx, ty + 1, kx + knob, ty + 1 + knob, 0xFFFFFFFF);
        }

        // футер
        ctx.fill(x, y + H - 18, x + W, y + H, 0xFF101015);
        ctx.drawTextWithShadow(textRenderer, Text.literal("sasavolk488"),
                x + 10, y + H - 13, 0xFF6A6A72);
        ctx.drawTextWithShadow(textRenderer, Text.literal("R — закрыть"),
                x + W - 70, y + H - 13, 0xFF4A4A52);
    }

    private void drawStar(DrawContext ctx, int x, int y, int color) {
        ctx.fill(x + 2, y, x + 4, y + 2, color);
        ctx.fill(x, y + 2, x + 6, y + 4, color);
        ctx.fill(x + 2, y + 4, x + 4, y + 6, color);
    }

    private int rainbow(int offset) {
        float hue = ((System.currentTimeMillis() % 4000L) / 4000f + offset * 0.08f) % 1f;
        return Color.HSBtoRGB(hue, 0.75f, 1f) | 0xFF000000;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = (int) guiX;
        int y = (int) guiY;

        // шапка — перетаскивание
        if (mx >= x && mx <= x + W && my >= y && my <= y + HEADER_H) {
            dragging = true;
            dragOffX = mx - guiX;
            dragOffY = my - guiY;
            return true;
        }

        // категории
        for (int i = 0; i < CATS.length; i++) {
            int cy2 = y + HEADER_H + 12 + i * 28;
            if (mx >= x + 4 && mx <= x + SIDEBAR_W - 4 && my >= cy2 && my <= cy2 + 22) {
                selectedCategory = i;
                return true;
            }
        }

        // модули
        int contentX = x + SIDEBAR_W + 8;
        int contentY = y + HEADER_H + 8;
        int contentW = W - SIDEBAR_W - 16;
        int colW = (contentW - COL_GAP) / 2;
        List<Module> mods = ModuleManager.byCategory(CATS[selectedCategory]);
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int col = i % 2;
            int row = i / 2;
            int cardX = contentX + col * (colW + COL_GAP);
            int cardY = contentY + row * (ROW_H + ROW_GAP);
            if (mx >= cardX && mx <= cardX + colW && my >= cardY && my <= cardY + ROW_H) {
                m.toggle();
                return true;
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            guiX = (float) (mx - dragOffX);
            guiY = (float) (my - dragOffY);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_R) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
                       }
