package com.example.client.gui;

import com.example.client.module.Module;
import com.example.client.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ClickGuiScreen extends Screen {

    // === GHOST STYLE COLORS ===
    private static final int COLOR_BG          = 0xE00A0A0C;
    private static final int COLOR_BG_HEADER   = 0xF0141418;
    private static final int COLOR_BG_SIDEBAR  = 0xF00F0F12;
    private static final int COLOR_ACCENT      = 0xFF4FC3F7;
    private static final int COLOR_TEXT        = 0xFFFFFFFF;
    private static final int COLOR_TEXT_DIM    = 0xFF888888;
    private static final int COLOR_CARD        = 0xE01A1A1E;
    private static final int COLOR_CARD_HOVER  = 0xE026262C;
    private static final int COLOR_TOGGLE_OFF  = 0xFF2A2A2E;
    private static final int COLOR_GEAR        = 0xFF333338;
    private static final int COLOR_BORDER      = 0xFF2A2A2E;

    // === SETTINGS PANEL COLORS ===
    private static final int BG_SETTINGS       = 0xF0141418;
    private static final int BG_SETTINGS_HD    = 0xFF0F0F12;
    private static final int BG_BTN            = 0xFF2E2E36;
    private static final int BG_BTN_HOVER      = 0xFF3E3E48;
    private static final int BG_SETTING_ROW    = 0xFF1E1E22;

    // === SIZES ===
    private static final int W = 420;
    private static final int H = 260;
    private static final int HEADER_H = 30;
    private static final int SIDEBAR_W = 100;
    private static final int ROW_H = 24;
    private static final int ROW_GAP = 6;
    private static final int COL_GAP = 6;
    private static final int SETTINGS_W = 220;
    private static final int SETTINGS_ROW_H = 26;

    private float guiX = 0;
    private float guiY = 0;

    private int selectedCategory = 0;
    private static final String[] CATS = {"Combat", "Movement", "Render", "Misc"};

    private boolean dragging = false;
    private double dragOffX, dragOffY;

    private long openTime;
    private Module settingsModule = null;

    public ClickGuiScreen() {
        super(Text.literal("ClickGUI"));
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        super.init();
        openTime = System.currentTimeMillis();
        guiX = (this.width - W) / 2f;
        guiY = (this.height - H) / 2f;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long elapsed = System.currentTimeMillis() - openTime;
        float t = Math.min(1f, elapsed / 200f);
        float openAnim = 1f - (1f - t) * (1f - t);

        ctx.fill(0, 0, width, height, ((int) (0x70 * openAnim)) << 24);

        for (Module m : ModuleManager.modules) {
            float target = m.enabled ? 1f : 0f;
            m.anim += (target - m.anim) * 0.2f;
        }

        float scale = 0.95f + 0.05f * openAnim;
        int cx = (int) guiX + W / 2;
        int cy = (int) guiY + H / 2;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(cx, cy, 0);
        ctx.getMatrices().scale(scale, scale, 1);
        ctx.getMatrices().translate(-cx, -cy, 0);

        drawWindow(ctx, mouseX, mouseY);
        if (settingsModule != null) drawSettingsPanel(ctx, mouseX, mouseY);

        ctx.getMatrices().pop();
    }

    private void drawWindow(DrawContext ctx, int mouseX, int mouseY) {
        int x = (int) guiX;
        int y = (int) guiY;

        ctx.fill(x + 6, y + 6, x + W + 6, y + H + 6, 0x60000000);
        ctx.fill(x, y, x + W, y + H, COLOR_BG);
        ctx.fill(x, y, x + W, y + HEADER_H, COLOR_BG_HEADER);
        ctx.fill(x, y, x + W, y + 1, COLOR_ACCENT);
        ctx.fill(x, y + HEADER_H, x + W, y + HEADER_H + 1, COLOR_BORDER);

        drawStar(ctx, x + 12, y + 12, COLOR_ACCENT);

        ctx.drawTextWithShadow(textRenderer, Text.literal("Client"), x + 28, y + 12, COLOR_TEXT_DIM);
        ctx.drawTextWithShadow(textRenderer, Text.literal(" / "), x + 60, y + 12, 0xFF444448);
        ctx.drawTextWithShadow(textRenderer, Text.literal(CATS[selectedCategory]), x + 72, y + 12, COLOR_TEXT);

        int sw = 100;
        int sx = x + W - sw - 10;
        ctx.fill(sx, y + 8, sx + sw, y + 22, 0xFF232326);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Search..."), sx + 8, y + 11, 0xFF555558);

        ctx.fill(x, y + HEADER_H, x + SIDEBAR_W, y + H, COLOR_BG_SIDEBAR);
        ctx.fill(x + SIDEBAR_W, y + HEADER_H, x + SIDEBAR_W + 1, y + H, COLOR_BORDER);

        for (int i = 0; i < CATS.length; i++) {
            int cy2 = y + HEADER_H + 14 + i * 28;
            boolean hover = mouseX >= x + 6 && mouseX <= x + SIDEBAR_W - 6 && mouseY >= cy2 && mouseY <= cy2 + 22;

            if (i == selectedCategory) {
                ctx.fill(x + 6, cy2, x + SIDEBAR_W - 6, cy2 + 22, 0xFF1E1E22);
                ctx.fill(x + 6, cy2, x + 9, cy2 + 22, COLOR_ACCENT);
            } else if (hover) {
                ctx.fill(x + 6, cy2, x + SIDEBAR_W - 6, cy2 + 22, 0xFF1A1A1E);
            }

            int textColor = i == selectedCategory ? COLOR_TEXT : COLOR_TEXT_DIM;
            ctx.drawTextWithShadow(textRenderer, Text.literal(CATS[i]), x + 20, cy2 + 7, textColor);
        }

        int contentX = x + SIDEBAR_W + 12;
        int contentY = y + HEADER_H + 12;
        int contentW = W - SIDEBAR_W - 24;
        int colW = (contentW - COL_GAP) / 2;

        List<Module> mods = ModuleManager.byCategory(CATS[selectedCategory]);

        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int col = i % 2;
            int row = i / 2;

            int cardX = contentX + col * (colW + COL_GAP);
            int cardY = contentY + row * (ROW_H + ROW_GAP);

            if (cardY + ROW_H > y + H - 12) break;

            boolean hover = mouseX >= cardX && mouseX <= cardX + colW && mouseY >= cardY && mouseY <= cardY + ROW_H;

            ctx.fill(cardX, cardY, cardX + colW, cardY + ROW_H, hover ? COLOR_CARD_HOVER : COLOR_CARD);

            int textColor = m.enabled ? COLOR_TEXT : COLOR_TEXT_DIM;
            ctx.drawTextWithShadow(textRenderer, Text.literal(m.name), cardX + 10, cardY + 8, textColor);

            int tsw = 24;
            int tsh = 12;

            if (!m.settings.isEmpty()) {
                int gearSize = 14;
                int gx = cardX + colW - gearSize - 6;
                int gy = cardY + (ROW_H - gearSize) / 2;

                int tx = gx - tsw - 5;
                int ty = cardY + (ROW_H - tsh) / 2;

                ctx.fill(tx, ty, tx + tsw, ty + tsh, COLOR_TOGGLE_OFF);
                if (m.anim > 0.01f) {
                    int fillW = Math.max(1, (int) (tsw * m.anim));
                    ctx.fill(tx, ty, tx + fillW, ty + tsh, COLOR_ACCENT);
                }
                int knob = tsh - 3;
                int kx = tx + 1 + (int) ((tsw - knob - 2) * m.anim);
                ctx.fill(kx, ty + 1, kx + knob, ty + 1 + knob, COLOR_TEXT);

                boolean gearHover = mouseX >= gx && mouseX <= gx + gearSize && mouseY >= gy && mouseY <= gy + gearSize;
                ctx.fill(gx, gy, gx + gearSize, gy + gearSize, gearHover ? COLOR_ACCENT : COLOR_GEAR);

                int cxx = gx + gearSize / 2;
                int cyy = gy + gearSize / 2;
                ctx.fill(cxx - 1, cyy - 3, cxx + 1, cyy + 3, COLOR_TEXT);
                ctx.fill(cxx - 3, cyy - 1, cxx + 3, cyy + 1, COLOR_TEXT);
                ctx.fill(cxx - 1, cyy - 1, cxx + 1, cyy + 1, COLOR_GEAR);
            } else {
                int tx = cardX + colW - tsw - 6;
                int ty = cardY + (ROW_H - tsh) / 2;

                ctx.fill(tx, ty, tx + tsw, ty + tsh, COLOR_TOGGLE_OFF);
                if (m.anim > 0.01f) {
                    int fillW = Math.max(1, (int) (tsw * m.anim));
                    ctx.fill(tx, ty, tx + fillW, ty + tsh, COLOR_ACCENT);
                }
                int knob = tsh - 3;
                int kx = tx + 1 + (int) ((tsw - knob - 2) * m.anim);
                ctx.fill(kx, ty + 1, kx + knob, ty + 1 + knob, COLOR_TEXT);
            }
        }
    }

    private void drawSettingsPanel(DrawContext ctx, int mouseX, int mouseY) {
        Module m = settingsModule;
        int listSize = m.settings.size();
        int panelW = SETTINGS_W;
        int panelH = 26 + listSize * SETTINGS_ROW_H + 8;

        int px = (this.width - panelW) / 2;
        int py = (this.height - panelH) / 2;

        ctx.fill(px + 4, py + 6, px + panelW + 4, py + panelH + 6, 0x80000000);
        ctx.fill(px, py, px + panelW, py + panelH, BG_SETTINGS);
        ctx.fill(px, py, px + panelW, py + 26, BG_SETTINGS_HD);
        ctx.fill(px, py, px + panelW, py + 1, COLOR_ACCENT);

        ctx.drawTextWithShadow(textRenderer, Text.literal(m.name), px + 10, py + 9, 0xFFFFFFFF);

        int closeX = px + panelW - 20;
        int closeY = py + 7;
        boolean closeHover = mouseX >= closeX && mouseX <= closeX + 14 && mouseY >= closeY && mouseY <= closeY + 14;
        ctx.fill(closeX, closeY, closeX + 14, closeY + 14, closeHover ? 0xFFAA3333 : 0xFF333338);
        ctx.drawTextWithShadow(textRenderer, Text.literal("X"), closeX + 4, closeY + 3, 0xFFFFFFFF);

        for (int i = 0; i < listSize; i++) {
            Module.Setting s = m.settings.get(i);
            int ry = py + 30 + i * SETTINGS_ROW_H;

            ctx.fill(px + 6, ry, px + panelW - 6, ry + SETTINGS_ROW_H - 4, BG_SETTING_ROW);
            ctx.drawTextWithShadow(textRenderer, Text.literal(s.name), px + 14, ry + 8, 0xFFDDDDEE);

            if (s.isBool) {
                int bw = 30, bh = 14;
                int bx = px + panelW - bw - 14;
                int by = ry + (SETTINGS_ROW_H - 4 - bh) / 2;

                ctx.fill(bx, by, bx + bw, by + bh, s.boolValue ? COLOR_ACCENT : 0xFF3A3A42);
                if (s.boolValue) {
                    ctx.fill(bx + bw - bh + 2, by + 2, bx + bw - 2, by + bh - 2, 0xFFFFFFFF);
                } else {
                    ctx.fill(bx + 2, by + 2, bx + bh - 2, by + bh - 2, 0xFFFFFFFF);
                }
            } else {
                int btnSize = 16;
                int plusX = px + panelW - 14 - btnSize;
                int minusX = plusX - btnSize - 40;
                int valueX = minusX + btnSize + 4;
                int by = ry + (SETTINGS_ROW_H - 4 - btnSize) / 2;

                boolean minusHover = mouseX >= minusX && mouseX <= minusX + btnSize && mouseY >= by && mouseY <= by + btnSize;
                boolean plusHover = mouseX >= plusX && mouseX <= plusX + btnSize && mouseY >= by && mouseY <= by + btnSize;

                ctx.fill(minusX, by, minusX + btnSize, by + btnSize, minusHover ? BG_BTN_HOVER : BG_BTN);
                ctx.drawTextWithShadow(textRenderer, Text.literal("-"), minusX + 6, by + 4, 0xFFFFFFFF);

                String val = (s.step >= 1f) ? String.valueOf((int) s.value) : String.format("%.1f", s.value);
                int vw = textRenderer.getWidth(val);
                ctx.drawTextWithShadow(textRenderer, Text.literal(val), valueX + (40 - vw) / 2, by + 4, 0xFFFFFFFF);

                ctx.fill(plusX, by, plusX + btnSize, by + btnSize, plusHover ? BG_BTN_HOVER : BG_BTN);
                ctx.drawTextWithShadow(textRenderer, Text.literal("+"), plusX + 5, by + 4, 0xFFFFFFFF);
            }
        }
    }

    private void drawStar(DrawContext ctx, int x, int y, int color) {
        ctx.fill(x + 2, y, x + 4, y + 2, color);
        ctx.fill(x, y + 2, x + 6, y + 4, color);
        ctx.fill(x + 2, y + 4, x + 4, y + 6, color);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (settingsModule != null) {
            Module m = settingsModule;
            int listSize = m.settings.size();
            int panelW = SETTINGS_W;
            int panelH = 26 + listSize * SETTINGS_ROW_H + 8;
            int px = (this.width - panelW) / 2;
            int py = (this.height - panelH) / 2;

            int closeX = px + panelW - 20;
            int closeY = py + 7;
            if (mx >= closeX && mx <= closeX + 14 && my >= closeY && my <= closeY + 14) {
                settingsModule = null;
                return true;
            }

            for (int i = 0; i < listSize; i++) {
                Module.Setting s = m.settings.get(i);
                int ry = py + 30 + i * SETTINGS_ROW_H;

                if (s.isBool) {
                    int bw = 30, bh = 14;
                    int bx = px + panelW - bw - 14;
                    int by = ry + (SETTINGS_ROW_H - 4 - bh) / 2;
                    if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) {
                        s.inc();
                        return true;
                    }
                } else {
                    int btnSize = 16;
                    int plusX = px + panelW - 14 - btnSize;
                    int minusX = plusX - btnSize - 40;
                    int by = ry + (SETTINGS_ROW_H - 4 - btnSize) / 2;

                    if (mx >= minusX && mx <= minusX + btnSize && my >= by && my <= by + btnSize) {
                        s.dec();
                        return true;
                    }
                    if (mx >= plusX && mx <= plusX + btnSize && my >= by && my <= by + btnSize) {
                        s.inc();
                        return true;
                    }
                }
            }

            return super.mouseClicked(mx, my, button);
        }

        int x = (int) guiX;
        int y = (int) guiY;

        if (mx >= x && mx <= x + W && my >= y && my <= y + HEADER_H) {
            dragging = true;
            dragOffX = mx - guiX;
            dragOffY = my - guiY;
            return true;
        }

        for (int i = 0; i < CATS.length; i++) {
            int cy2 = y + HEADER_H + 14 + i * 28;
            if (mx >= x + 6 && mx <= x + SIDEBAR_W - 6 && my >= cy2 && my <= cy2 + 22) {
                selectedCategory = i;
                return true;
            }
        }

        int contentX = x + SIDEBAR_W + 12;
        int contentY = y + HEADER_H + 12;
        int contentW = W - SIDEBAR_W - 24;
        int colW = (contentW - COL_GAP) / 2;
        List<Module> mods = ModuleManager.byCategory(CATS[selectedCategory]);

        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int col = i % 2;
            int row = i / 2;
            int cardX = contentX + col * (colW + COL_GAP);
            int cardY = contentY + row * (ROW_H + ROW_GAP);
            if (cardY + ROW_H > y + H - 12) break;

            if (mx >= cardX && mx <= cardX + colW && my >= cardY && my <= cardY + ROW_H) {
                if (button == 1) {
                    if (!m.settings.isEmpty()) settingsModule = m;
                    return true;
                }

                if (!m.settings.isEmpty()) {
                    int gearSize = 14;
                    int gx = cardX + colW - gearSize - 6;
                    int gy = cardY + (ROW_H - gearSize) / 2;
                    if (mx >= gx && mx <= gx + gearSize && my >= gy && my <= gy + gearSize) {
                        settingsModule = m;
                        return true;
                    }
                }

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
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (settingsModule != null) {
                settingsModule = null;
                return true;
            }
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    }
