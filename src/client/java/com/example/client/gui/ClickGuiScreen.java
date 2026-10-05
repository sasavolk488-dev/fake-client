package com.example.client.gui;

import com.example.client.module.Module;
import com.example.client.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ClickGuiScreen extends Screen {

    private static final int W = 380;
    private static final int H = 240;
    private static final int HEADER_H = 26;
    private static final int SIDEBAR_W = 85;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 5;
    private static final int COL_GAP = 5;

    private static final int BG_MAIN       = 0xFF23232A;
    private static final int BG_HEADER     = 0xFF1B1B20;
    private static final int BG_SIDEBAR    = 0xFF1B1B20;
    private static final int BG_ROW        = 0xFF2B2B32;
    private static final int BG_ROW_HOVER  = 0xFF33333A;
    private static final int BG_SEARCH     = 0xFF25252C;
    private static final int BG_CAT_ACTIVE = 0xFF26262E;
    private static final int BG_CAT_HOVER  = 0xFF1F1F26;
    private static final int BG_TOGGLE_OFF = 0xFF3A3A42;

    private float guiX = 0;
    private float guiY = 0;

    private int selectedCategory = 0;
    private static final String[] CATS = {"Combat", "Movement", "Render", "Misc"};

    private boolean dragging = false;
    private double dragOffX, dragOffY;

    private long openTime;

    public ClickGuiScreen() {
        super(Text.literal("ClickGUI"));
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        super.init();
        openTime = System.currentTimeMillis();
        centerGui();
    }

    private void centerGui() {
        guiX = (this.width - W) / 2f;
        guiY = (this.height - H) / 2f;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long elapsed = System.currentTimeMillis() - openTime;
        float t = Math.min(1f, elapsed / 180f);
        float openAnim = 1f - (1f - t) * (1f - t);

        ctx.fill(0, 0, width, height, ((int) (0x50 * openAnim)) << 24);

        for (Module m : ModuleManager.modules) {
            float target = m.enabled ? 1f : 0f;
            m.anim += (target - m.anim) * 0.25f;
        }

        float scale = 0.96f + 0.04f * openAnim;
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

        ctx.fill(x + 3, y + 4, x + W + 3, y + H + 4, 0x60000000);
        ctx.fill(x, y, x + W, y + H, BG_MAIN);
        ctx.fill(x, y, x + W, y + HEADER_H, BG_HEADER);

        drawStar(ctx, x + 9, y + 9, 0xFFFFFFFF);

        ctx.drawTextWithShadow(textRenderer, Text.literal("Client"), x + 24, y + 9, 0xFF77778A);
        ctx.drawTextWithShadow(textRenderer, Text.literal(" / "), x + 56, y + 9, 0xFF3A3A42);
        ctx.drawTextWithShadow(textRenderer, Text.literal(CATS[selectedCategory]),
                x + 68, y + 9, 0xFFFFFFFF);

        int sw = 95;
        int sx = x + W - sw - 8;
        ctx.fill(sx, y + 6, sx + sw, y + 20, BG_SEARCH);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Search..."),
                sx + 6, y + 9, 0xFF56565E);

        ctx.fill(x, y + HEADER_H, x + SIDEBAR_W, y + H, BG_SIDEBAR);
        ctx.fill(x + SIDEBAR_W, y + HEADER_H, x + SIDEBAR_W + 1, y + H, 0xFF16161A);

        for (int i = 0; i < CATS.length; i++) {
            int cy2 = y + HEADER_H + 10 + i * 25;
            boolean hover = mouseX >= x + 4 && mouseX <= x + SIDEBAR_W - 4
                    && mouseY >= cy2 && mouseY <= cy2 + 20;

            if (i == selectedCategory) {
                ctx.fill(x + 4, cy2, x + SIDEBAR_W - 4, cy2 + 20, BG_CAT_ACTIVE);
            } else if (hover) {
                ctx.fill(x + 4, cy2, x + SIDEBAR_W - 4, cy2 + 20, BG_CAT_HOVER);
            }

            int dotColor = i == selectedCategory ? 0xFFFFFFFF : 0xFF5A5A62;
            ctx.fill(x + 11, cy2 + 8, x + 15, cy2 + 12, dotColor);

            int textColor = i == selectedCategory ? 0xFFFFFFFF : 0xFF9A9AA2;
            ctx.drawTextWithShadow(textRenderer, Text.literal(CATS[i]),
                    x + 22, cy2 + 6, textColor);
        }

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

            ctx.fill(cardX, cardY, cardX + colW, cardY + ROW_H,
                    hover ? BG_ROW_HOVER : BG_ROW);

            int textColor = m.enabled ? 0xFFFFFFFF : 0xFFAAAAAA;
            ctx.drawTextWithShadow(textRenderer, Text.literal(m.name),
                    cardX + 7, cardY + 6, textColor);

            int tsw = 20;
            int tsh = 11;
            int tx = cardX + colW - tsw - 6;
            int ty = cardY + (ROW_H - tsh) / 2;

            ctx.fill(tx, ty, tx + tsw, ty + tsh, BG_TOGGLE_OFF);

            if (m.anim > 0.01f) {
                int fillW = Math.max(1, (int) (tsw * m.anim));
                ctx.fill(tx, ty, tx + fillW, ty + tsh, 0xFFFFFFFF);
            }

            int knob = tsh - 3;
            int kx = tx + 1 + (int) ((tsw - knob - 2) * m.anim);
            int knobColor = m.anim > 0.5f ? 0xFF23232A : 0xFF888888;
            ctx.fill(kx, ty + 1, kx + knob, ty + 1 + knob, knobColor);
        }
    }

    private void drawStar(DrawContext ctx, int x, int y, int color) {
        ctx.fill(x + 2, y, x + 4, y + 2, color);
        ctx.fill(x, y + 2, x + 6, y + 4, color);
        ctx.fill(x + 2, y + 4, x + 4, y + 6, color);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = (int) guiX;
        int y = (int) guiY;

        if (mx >= x && mx <= x + W && my >= y && my <= y + HEADER_H) {
            dragging = true;
            dragOffX = mx - guiX;
            dragOffY = my - guiY;
            return true;
        }

        for (int i = 0; i < CATS.length; i++) {
            int cy2 = y + HEADER_H + 10 + i * 25;
            if (mx >= x + 4 && mx <= x + SIDEBAR_W - 4 && my >= cy2 && my <= cy2 + 20) {
                selectedCategory = i;
                return true;
            }
        }

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
