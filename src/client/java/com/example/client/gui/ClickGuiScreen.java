package com.example.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends Screen {

    private static final int W = 400;
    private static final int H = 280;
    private static final int HEADER_H = 28;
    private static final int SIDEBAR_W = 110;
    private static final int ROW_H = 38;

    private float guiX = 20, guiY = 20;
    private long openTime;
    private float openAnim = 0f;

    private int selectedTab = 0;
    private int selectedCategory = 0;

    private static final String[] TABS = {"Modules", "Favorites", "Configs"};
    private static final String[] CATS = {"Combat", "Movement", "Render", "Player", "Misc"};

    public static class Mod {
        String name;
        boolean enabled;
        float anim;
        Mod(String n, boolean e) { name = n; enabled = e; anim = e ? 1f : 0f; }
    }

    private static final List<List<Mod>> MODULES = new ArrayList<>();
    static {
        List<Mod> combat = new ArrayList<>();
        combat.add(new Mod("KillAura", true));
        combat.add(new Mod("AutoClicker", true));
        combat.add(new Mod("Reach", false));
        combat.add(new Mod("Velocity", true));
        combat.add(new Mod("Criticals", false));
        MODULES.add(combat);

        List<Mod> movement = new ArrayList<>();
        movement.add(new Mod("Sprint", true));
        movement.add(new Mod("Fly", false));
        movement.add(new Mod("Speed", true));
        movement.add(new Mod("NoFall", false));
        movement.add(new Mod("Jesus", false));
        MODULES.add(movement);

        List<Mod> render = new ArrayList<>();
        render.add(new Mod("ESP", true));
        render.add(new Mod("Tracers", false));
        render.add(new Mod("Fullbright", true));
        render.add(new Mod("Xray", false));
        MODULES.add(render);

        List<Mod> player = new ArrayList<>();
        player.add(new Mod("ChestStealer", true));
        player.add(new Mod("FastPlace", true));
        player.add(new Mod("AutoTool", false));
        MODULES.add(player);

        List<Mod> misc = new ArrayList<>();
        misc.add(new Mod("Hud", true));
        misc.add(new Mod("AntiAFK", true));
        misc.add(new Mod("NameProtect", false));
        MODULES.add(misc);
    }

    private boolean dragging = false;
    private double dragOffX, dragOffY;

    public ClickGuiScreen() {
        super(Text.literal("ClickGUI"));
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        super.init();
        openTime = System.currentTimeMillis();
        openAnim = 0f;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long elapsed = System.currentTimeMillis() - openTime;
        float t = Math.min(1f, elapsed / 220f);
        openAnim = 1f - (1f - t) * (1f - t);

        int alpha = (int)(0x60 * openAnim);
        ctx.fill(0, 0, width, height, alpha << 24);

        for (List<Mod> list : MODULES) {
            for (Mod m : list) {
                float target = m.enabled ? 1f : 0f;
                m.anim += (target - m.anim) * 0.2f;
            }
        }

        float scale = 0.93f + 0.07f * openAnim;
        int cx = (int)guiX + W / 2;
        int cy = (int)guiY + H / 2;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(cx, cy, 0);
        ctx.getMatrices().scale(scale, scale, 1);
        ctx.getMatrices().translate(-cx, -cy, 0);

        drawWindow(ctx, mouseX, mouseY);

        ctx.getMatrices().pop();
    }

    private void drawWindow(DrawContext ctx, int mouseX, int mouseY) {
        int x = (int)guiX;
        int y = (int)guiY;

        ctx.fill(x + 6, y + 8, x + W + 6, y + H + 8, 0x70000000);
        ctx.fill(x, y, x + W, y + H, 0xF0121216);
        ctx.fill(x, y, x + W, y + HEADER_H, 0xFF19191E);

        for (int i = 0; i < W; i++) {
            float hue = ((System.currentTimeMillis() % 4000L) / 4000f + i * 0.004f) % 1f;
            int col = Color.HSBtoRGB(hue, 0.7f, 1f) | 0xFF000000;
            ctx.fill(x + i, y, x + i + 1, y + 3, col);
        }

        ctx.drawTextWithShadow(textRenderer, Text.literal("ClickGUI"),
                x + 10, y + 10, 0xFFFFFFFF);

        int tabX = x + 90;
        for (int i = 0; i < TABS.length; i++) {
            String tab = TABS[i];
            int tw = textRenderer.getWidth(tab) + 12;
            boolean hover = mouseX >= tabX && mouseX <= tabX + tw
                    && mouseY >= y + 5 && mouseY <= y + HEADER_H - 5;
            if (i == selectedTab) {
                ctx.fill(tabX, y + 5, tabX + tw, y + HEADER_H - 5, 0xFF2A2A36);
                ctx.fill(tabX, y + HEADER_H - 7, tabX + tw, y + HEADER_H - 5, rainbow(0));
            } else if (hover) {
                ctx.fill(tabX, y + 5, tabX + tw, y + HEADER_H - 5, 0xFF1F1F28);
            }
            int txtColor = i == selectedTab ? 0xFFFFFFFF : 0xFFAAAAAA;
            ctx.drawTextWithShadow(textRenderer, Text.literal(tab),
                    tabX + 6, y + 10, txtColor);
            tabX += tw + 2;
        }

        ctx.fill(x, y + HEADER_H, x + SIDEBAR_W, y + H, 0xFF16161A);
        ctx.fill(x + SIDEBAR_W, y + HEADER_H, x + SIDEBAR_W + 1, y + H, 0xFF2A2A32);

        for (int i = 0; i < CATS.length; i++) {
            int cy = y + HEADER_H + 10 + i * 32;
            int cw = SIDEBAR_W - 12;
            int cx2 = x + 6;
            boolean hover = mouseX >= cx2 && mouseX <= cx2 + cw
                    && mouseY >= cy && mouseY <= cy + 26;

            if (i == selectedCategory) {
                ctx.fill(cx2, cy, cx2 + cw, cy + 26, 0xFF2E2E3A);
                ctx.fill(cx2, cy, cx2 + 4, cy + 26, rainbow(0));
            } else if (hover) {
                ctx.fill(cx2, cy, cx2 + cw, cy + 26, 0xFF1F1F26);
            }

            int textColor = i == selectedCategory ? 0xFFFFFFFF : 0xFFB0B0B0;
            ctx.drawTextWithShadow(textRenderer, Text.literal(CATS[i]),
                    cx2 + 14, cy + 9, textColor);
        }

        int listX = x + SIDEBAR_W + 14;
        int listY = y + HEADER_H + 20;
        int rowW = W - SIDEBAR_W - 28;

        ctx.drawTextWithShadow(textRenderer, Text.literal(CATS[selectedCategory]),
                listX, listY - 14, rainbow(0));

        List<Mod> mods = MODULES.get(selectedCategory);
        for (int i = 0; i < mods.size(); i++) {
            Mod m = mods.get(i);
            int my = listY + i * (ROW_H + 4);
            if (my + ROW_H > y + H - 24) break;

            boolean hover = mouseX >= listX && mouseX <= listX + rowW
                    && mouseY >= my && mouseY <= my + ROW_H;

            int bg = hover ? 0xFF25252E : 0xFF1E1E24;
            ctx.fill(listX, my, listX + rowW, my + ROW_H, bg);

            int col = m.enabled ? rainbow(0) : 0xFF3C3C46;
            ctx.fill(listX, my, listX + 4, my + ROW_H, col);

            int nc = m.enabled ? 0xFFFFFFFF : 0xFFA0A0A0;
            ctx.drawTextWithShadow(textRenderer, Text.literal(m.name),
                    listX + 16, my + 15, nc);

            int sw = 36, sh = 16;
            int sx = listX + rowW - sw - 12;
            int sy = my + (ROW_H - sh) / 2;

            ctx.fill(sx, sy, sx + sw, sy + sh, 0xFF3C3C46);

            if (m.anim > 0.01f) {
                int fillW = (int)(sw * m.anim);
                ctx.fill(sx, sy, sx + fillW, sy + sh, rainbow(0));
            }

            int knob = sh - 4;
            int kx = sx + 2 + (int)((sw - knob - 4) * m.anim);
            ctx.fill(kx, sy + 2, kx + knob, sy + 2 + knob, 0xFFFFFFFF);
        }

        ctx.fill(x, y + H - 20, x + W, y + H, 0xFF16161A);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Right Shift — закрыть"),
                x + 10, y + H - 14, 0xFF666666);
    }

    private int rainbow(int offset) {
        float hue = ((System.currentTimeMillis() % 4000L) / 4000f + offset * 0.08f) % 1f;
        return Color.HSBtoRGB(hue, 0.75f, 1f) | 0xFF000000;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = (int)guiX;
        int y = (int)guiY;

        if (mx >= x && mx <= x + W && my >= y && my <= y + HEADER_H) {
            int tabX = x + 90;
            for (int i = 0; i < TABS.length; i++) {
                int tw = textRenderer.getWidth(TABS[i]) + 12;
                if (mx >= tabX && mx <= tabX + tw) {
                    selectedTab = i;
                    return true;
                }
                tabX += tw + 2;
            }
            dragging = true;
            dragOffX = mx - guiX;
            dragOffY = my - guiY;
            return true;
        }

        for (int i = 0; i < CATS.length; i++) {
            int cy = y + HEADER_H + 10 + i * 32;
            int cw = SIDEBAR_W - 12;
            int cx2 = x + 6;
            if (mx >= cx2 && mx <= cx2 + cw && my >= cy && my <= cy + 26) {
                selectedCategory = i;
                return true;
            }
        }

        int listX = x + SIDEBAR_W + 14;
        int listY = y + HEADER_H + 20;
        int rowW = W - SIDEBAR_W - 28;
        List<Mod> mods = MODULES.get(selectedCategory);
        for (int i = 0; i < mods.size(); i++) {
            int my2 = listY + i * (ROW_H + 4);
            if (mx >= listX && mx <= listX + rowW && my >= my2 && my <= my2 + ROW_H) {
                mods.get(i).enabled = !mods.get(i).enabled;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            guiX = (float)(mx - dragOffX);
            guiY = (float)(my - dragOffY);
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
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
            }
