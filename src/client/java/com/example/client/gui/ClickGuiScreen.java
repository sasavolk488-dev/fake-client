 else {
                ctx.fill(tabX, tabY, tabX + tw, tabY + 18, 0xFF282830);
                ctx.drawTextWithShadow(textRenderer, Text.literal(name),
                        tabX + 6, tabY + 5, 0xFFD0D0D0);
            }
            tabX +=  + 3;
        }

        List<Mod> mods = CATEGORIES.get(selectedTab).mods;
        int listY = y + 52;
        int rowH = 20;

        for (int i = 0; i < mods.size(); i++) {
            Mod m = mods.get(i);
            int my = listY + i * rowH;

            float target = m.enabled ? 1f : 0f;
            m.anim += (target - m.anim) * 0.25f;

            ctx.fill(x + 8, my, x + W - 8, my + rowH - 3, 0xFF1E1E24);

            int indW = (int) (3 * m.anim);
            if (indW > 0)
                ctx.fill(x + 8, my, x + 8 + indW, my + rowH - 3, rainbow(0, 0.1f));

            ctx.drawTextWithShadow(textRenderer, Text.literal(m.name),
                    x + 16, my + 6, m.anim > 0.5f ? 0xFFFFFFFF : 0xFFA0A0A0);

            int sw = 26, sh = 10;
            int sx = x + W - 16 - sw;
            int sy = my + (rowH - 3 - sh) / 2;

            ctx.fill(sx, sy, sx + sw, sy + sh, 0xFF3C3C46);
            int fill = (int) ((sw - 2) * m.anim);
            if (fill > 0)
                ctx.fill(sx + 1, sy + 1, sx + 1 + fill, sy + sh - 1, rainbow(0, 0.1f));

            int knob = sh - 2;
            int kx = sx + 1 + (int) ((sw - 2 - knob) * m.anim);
            ctx.fill(kx, sy + 1, kx + knob, sy + 1 + knob, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (mx >= guiX && mx <= guiX + W && my >= guiY && my <= guiY + 24) {
            dragging = true;
            dragOffX = (int) mx - guiX;
            dragOffY = (int) my - guiY;
            return true;
        }

        int tabX = guiX + 6;
        int tabY = guiY + 28;
        for (int i = 0; i < CATEGORIES.size(); i++) {
            int tw = textRenderer.getWidth(CATEGORIES.get(i).name) + 12;
            if (mx >= tabX && mx <= tabX + tw && my >= tabY && my <= tabY + 18) {
                selectedTab = i;
                return true;
            }
            tabX += tw + 3;
        }

        List<Mod> mods = CATEGORIES.get(selectedTab).mods;
        int listY = guiY + 52;
        int rowH = 20;
        for (int i = 0; i < mods.size(); i++) {
            int my2 = listY + i * rowH;
            int sw = 26;
            int sx = guiX + W - 16 - sw;
            int sy = my2 + (rowH - 3 - 10) / 2;
            if (mx >= sx && mx <= sx + sw && my >= sy && my <= sy + 10) {
                Mod m = mods.get(i);
                m.enabled = !m.enabled;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            guiX = (int) mx - dragOffX;
            guiY = (int) my - dragOffY;
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
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static int rainbow(int offset, float speed) {
        double t = Util.getMillis() / 4000.0;
        float hue = (float) ((t + offset * 0.08 * speed) % 1.0);
        return Color.HSBtoRGB(hue, 0.75f, 1f) | 0xFF000000;
package com.example.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends Screen {

    public static class Mod {
        public final String name;
        public boolean enabled;
        public float anim;
        public Mod(String name, boolean enabled) {
            this.name = name;
            this.enabled = enabled;
            this.anim = enabled ? 1f : 0f;
        }
    }

    public static class Category {
        public final String name;
        public final List<Mod> mods = new ArrayList<>();
        public Category(String name) { this.name = name; }
    }

    private static final List<Category> CATEGORIES = new ArrayList<>();
    static {
        Category combat = new Category("Combat");
        combat.mods.add(new Mod("KillAura", true));
        combat.mods.add(new Mod("AutoClicker", true));
        combat.mods.add(new Mod("Reach", false));

        Category movement = new Category("Movement");
        movement.mods.add(new Mod("Sprint", true));
        movement.mods.add(new Mod("Fly", false));
        movement.mods.add(new Mod("Speed", true));

        Category render = new Category("Render");
        render.mods.add(new Mod("ESP", true));
        render.mods.add(new Mod("Tracers", false));

        Category misc = new Category("Misc");
        misc.mods.add(new Mod("Hud", true));
        misc.mods.add(new Mod("AntiAFK", true));

        CATEGORIES.add(combat);
        CATEGORIES.add(movement);
        CATEGORIES.add(render);
        CATEGORIES.add(misc);
    }

    private int selectedTab = 0;
    private int guiX = 40, guiY = 40;
    private boolean dragging = false;
    private int dragOffX, dragOffY;

    private static final int W = 320, H = 240;

    public ClickGuiScreen() {
        super(Text.literal("ClickGUI"));
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x40000000);

        int x = guiX, y = guiY;
        ctx.fill(x, y, x + W, y + H, 0xE6121216);
        ctx.fill(x, y, x + W, y + 24, 0xFF19191E);

        for (int i = 0; i < W; i++) {
            ctx.fill(x + i, y, x + i + 1, y + 2, rainbow(i, 0.003f));
        }

        ctx.drawTextWithShadow(textRenderer, Text.literal("ClickGUI"),
                x + 8, y + 8, 0xFFFFFFFF);

        int tabX = x + 6;
        int tabY = y + 28;
        for (int i = 0; i < CATEGORIES.size(); i++) {
            String name = CATEGORIES.get(i).name;
            int tw = textRenderer.getWidth(name) + 12;
            if (i == selectedTab) {
                ctx.fill(tabX, tabY, tabX + tw, tabY + 18, rainbow(i, 0.1f));
                ctx.drawTextWithShadow(textRenderer, Text.literal(name),
                        tabX + 6, tabY + 5, 0xFF000000);
            } else {
                ctx.fill(tabX, tabY, tabX + tw, tabY + 18, 0xFF282830);
                ctx.drawTextWithShadow(textRenderer, Text.literal(name),
                        tabX + 6, tabY + 5, 0xFFD0D0D0);
            }
            tabX += tw + 3;
        }

        List<Mod> mods = CATEGORIES.get(selectedTab).mods;
        int listY = y + 52;
        int rowH = 20;

        for (int i = 0; i < mods.size(); i++) {
            Mod m = mods.get(i);
            int my = listY + i * rowH;

            float target = m.enabled ? 1f : 0f;
            m.anim += (target - m.anim) * 0.25f;

            ctx.fill(x + 8, my, x + W - 8, my + rowH - 3, 0xFF1E1E24);

            int indW = (int) (3 * m.anim);
            if (indW > 0)
                ctx.fill(x + 8, my, x + 8 + indW, my + rowH - 3, rainbow(0, 0.1f));

            ctx.drawTextWithShadow(textRenderer, Text.literal(m.name),
                    x + 16, my + 6, m.anim > 0.5f ? 0xFFFFFFFF : 0xFFA0A0A0);

            int sw = 26, sh = 10;
            int sx = x + W - 16 - sw;
            int sy = my + (rowH - 3 - sh) / 2;

            ctx.fill(sx, sy, sx + sw, sy + sh, 0xFF3C3C46);
            int fill = (int) ((sw - 2) * m.anim);
            if (fill > 0)
                ctx.fill(sx + 1, sy + 1, sx + 1 + fill, sy + sh - 1, rainbow(0, 0.1f));

            int knob = sh - 2;
            int kx = sx + 1 + (int) ((sw - 2 - knob) * m.anim);
            ctx.fill(kx, sy + 1, kx + knob, sy + 1 + knob, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (mx >= guiX && mx <= guiX + W && my >= guiY && my <= guiY + 24) {
            dragging = true;
            dragOffX = (int) mx - guiX;
            dragOffY = (int) my - guiY;
            return true;
        }

        int tabX = guiX + 6;
        int tabY = guiY + 28;
        for (int i = 0; i < CATEGORIES.size(); i++) {
            int tw = textRenderer.getWidth(CATEGORIES.get(i).name) + 12;
            if (mx >= tabX && mx <= tabX + tw && my >= tabY && my <= tabY + 18) {
                selectedTab = i;
                return true;
            }
            tabX += tw + 3;
        }

        List<Mod> mods = CATEGORIES.get(selectedTab).mods;
        int listY = guiY + 52;
        int rowH = 20;
        for (int i = 0; i < mods.size(); i++) {
            int my2 = listY + i * rowH;
            int sw = 26;
            int sx = guiX + W - 16 - sw;
            int sy = my2 + (rowH - 3 - 10) / 2;
            if (mx >= sx && mx <= sx + sw && my >= sy && my <= sy + 10) {
                Mod m = mods.get(i);
                m.enabled = !m.enabled;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            guiX = (int) mx - dragOffX;
            guiY = (int) my - dragOffY;
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
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static int rainbow(int offset, float speed) {
        double t = (System.currentTimeMillis() % 4000L) / 4000.0;
        float hue = (float) ((t + offset * 0.08 * speed) % 1.0);
        return Color.HSBtoRGB
