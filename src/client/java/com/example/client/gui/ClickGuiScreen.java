package com.example.client.gui;

import com.example.client.module.Module;
import com.example.client.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ClickGuiScreen extends Screen {

    // ===== LIMINAR COLORS =====
    private static final int C_BG          = 0xFF0D0D14;
    private static final int C_SIDEBAR     = 0xFF0A0A10;
    private static final int C_MIDDLE      = 0xFF0F0F18;
    private static final int C_RIGHT       = 0xFF0D0D18;
    private static final int C_BORDER      = 0xFF1A1A28;
    private static final int C_ACCENT      = 0xFF3BB8F0;
    private static final int C_ACCENT_DIM  = 0xFF1A5A7A;
    private static final int C_TEXT        = 0xFFFFFFFF;
    private static final int C_TEXT_DIM    = 0xFF8888A0;
    private static final int C_HEADER      = 0xFF6A6A8A;
    private static final int C_ROW_HOVER   = 0xFF1A1A28;
    private static final int C_ROW_ACTIVE  = 0xFF14141F;
    private static final int C_TOGGLE_OFF  = 0xFF2A2A3A;
    private static final int C_SLIDER_BG   = 0xFF252535;
    private static final int C_SEARCH_BG   = 0xFF14141E;

    // ===== SIZES =====
    private static final int W = 380;
    private static final int H = 220;
    private static final int SIDEBAR_W = 85;
    private static final int MIDDLE_W = 140;
    private static final int RIGHT_W = W - SIDEBAR_W - MIDDLE_W - 2;

    private float guiX = 0;
    private float guiY = 0;

    private int selectedCategory = 0;
    private static final String[] CATS = {"Combat", "Movement", "Render", "Player", "Utilities", "Themes", "Configs"};

    private Module selectedModule = null;
    private boolean dragging = false;
    private double dragOffX, dragOffY;
    private long openTime;

    // Slider drag state
    private Module.Setting dragSlider = null;
    private int dragSliderX = 0, dragSliderW = 0;

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
        float t = Math.min(1f, elapsed / 180f);
        float anim = 1f - (1f - t) * (1f - t);

        ctx.fill(0, 0, width, height, ((int) (0x70 * anim)) << 24);

        for (Module m : ModuleManager.modules) {
            float target = m.enabled ? 1f : 0f;
            m.anim += (target - m.anim) * 0.2f;
        }

        float scale = 0.95f + 0.05f * anim;
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

        ctx.fill(x + 4, y + 5, x + W + 4, y + H + 5, 0x80000000);
        ctx.fill(x, y, x + W, y + H, C_BG);

        // ============ SIDEBAR ============
        ctx.fill(x, y, x + SIDEBAR_W, y + H, C_SIDEBAR);
        ctx.fill(x + SIDEBAR_W, y, x + SIDEBAR_W + 1, y + H, C_BORDER);

        // Logo
        int lx = x + 6, ly = y + 6;
        ctx.fill(lx, ly, lx + 18, ly + 18, C_ACCENT_DIM);
        ctx.drawTextWithShadow(textRenderer, Text.literal("L"), lx + 6, ly + 6, C_ACCENT);

        ctx.drawTextWithShadow(textRenderer, Text.literal("Liminar"), x + 30, y + 6, C_TEXT);
        ctx.drawTextWithShadow(textRenderer, Text.literal("1.21.4"), x + 30, y + 16, C_TEXT);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Free"), x + 30, y + 24, C_ACCENT);

        ctx.fill(x + 6, y + 40, x + SIDEBAR_W - 6, y + 41, C_BORDER);

        // Categories
        for (int i = 0; i < CATS.length; i++) {
            int cy2 = y + 46 + i * 20;
            boolean hover = mouseX >= x && mouseX <= x + SIDEBAR_W && mouseY >= cy2 && mouseY <= cy2 + 18;

            if (i == selectedCategory) {
                ctx.fill(x + 2, cy2, x + SIDEBAR_W - 2, cy2 + 18, C_ROW_ACTIVE);
                ctx.fill(x + 2, cy2, x + 4, cy2 + 18, C_ACCENT);
            } else if (hover) {
                ctx.fill(x + 2, cy2, x + SIDEBAR_W - 2, cy2 + 18, C_ROW_HOVER);
            }

            int dotColor = i == selectedCategory ? C_ACCENT : C_TEXT_DIM;
            ctx.fill(x + 12, cy2 + 6, x + 18, cy2 + 12, dotColor);

            int tc = i == selectedCategory ? C_TEXT : C_TEXT_DIM;
            ctx.drawTextWithShadow(textRenderer, Text.literal(CATS[i]), x + 24, cy2 + 5, tc);
        }

        // Search
        int sy = y + H - 22;
        ctx.fill(x + 4, sy, x + SIDEBAR_W - 4, sy + 16, C_SEARCH_BG);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Поиск..."), x + 10, sy + 4, C_TEXT_DIM);

        // ============ MIDDLE ============
        int mx = x + SIDEBAR_W + 1;
        ctx.fill(mx, y, mx + MIDDLE_W, y + H, C_MIDDLE);

        // Header
        ctx.drawTextWithShadow(textRenderer, Text.literal(CATS[selectedCategory]), mx + 8, y + 7, C_TEXT);

        List<Module> mods = ModuleManager.byCategory(CATS[selectedCategory]);
        int enabledCount = 0;
        for (Module m : mods) if (m.enabled) enabledCount++;
        ctx.drawTextWithShadow(textRenderer, Text.literal(enabledCount + "/" + mods.size() + " вкл"),
                mx + 8, y + 18, C_TEXT_DIM);

        ctx.fill(mx, y + 32, mx + MIDDLE_W, y + 33, C_BORDER);

        // Module rows
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int ry = y + 36 + i * 20;
            if (ry + 18 > y + H - 4) break;

            boolean hover = mouseX >= mx && mouseX <= mx + MIDDLE_W && mouseY >= ry && mouseY <= ry + 18;
            boolean isSelected = (selectedModule == m);

            if (isSelected) {
                ctx.fill(mx, ry, mx + MIDDLE_W, ry + 18, C_ROW_ACTIVE);
            } else if (hover) {
                ctx.fill(mx, ry, mx + MIDDLE_W, ry + 18, C_ROW_HOVER);
            }

            int tc = m.enabled ? C_TEXT : C_TEXT_DIM;
            ctx.drawTextWithShadow(textRenderer, Text.literal(m.name), mx + 8, ry + 5, tc);

            // Round toggle with checkmark
            int tr = 5;
            int tx = mx + MIDDLE_W - 12;
            int ty = ry + 9;

            ctx.fill(tx - tr, ty - tr, tx + tr, ty + tr, m.enabled ? C_ACCENT : C_TOGGLE_OFF);

            if (m.enabled) {
                // Checkmark (white)
                ctx.fill(tx - 2, ty, tx - 1, ty + 2, 0xFFFFFFFF);
                ctx.fill(tx - 1, ty + 1, tx, ty + 3, 0xFFFFFFFF);
                ctx.fill(tx, ty, tx + 1, ty + 2, 0xFFFFFFFF);
                ctx.fill(tx + 1, ty - 1, tx + 2, ty + 1, 0xFFFFFFFF);
            }
        }

        // ============ RIGHT ============
        int rx = mx + MIDDLE_W + 1;
        ctx.fill(rx, y, rx + RIGHT_W, y + H, C_RIGHT);
        ctx.fill(rx - 1, y, rx, y + H, C_BORDER);

        ctx.drawTextWithShadow(textRenderer, Text.literal("Настройки"), rx + 8, y + 7, C_HEADER);
        ctx.fill(rx, y + 20, rx + RIGHT_W, y + 21, C_BORDER);

        if (selectedModule != null && !selectedModule.settings.isEmpty()) {
            int ry2 = y + 28;
            for (Module.Setting s : selectedModule.settings) {
                if (ry2 + 24 > y + H - 4) break;

                if (s.isBool) {
                    // Toggle row
                    ctx.drawTextWithShadow(textRenderer, Text.literal(s.name), rx + 8, ry2 + 4, C_TEXT);

                    int tr = 5;
                    int tx = rx + RIGHT_W - 12;
                    int ty = ry2 + 9;

                    ctx.fill(tx - tr, ty - tr, tx + tr, ty + tr, s.boolValue ? C_ACCENT : C_TOGGLE_OFF);

                    if (s.boolValue) {
                        ctx.fill(tx - 2, ty, tx - 1, ty + 2, 0xFFFFFFFF);
                        ctx.fill(tx - 1, ty + 1, tx, ty + 3, 0xFFFFFFFF);
                        ctx.fill(tx, ty, tx + 1, ty + 2, 0xFFFFFFFF);
                        ctx.fill(tx + 1, ty - 1, tx + 2, ty + 1, 0xFFFFFFFF);
                    }
                    ry2 += 24;
                } else {
                    // Slider
                    ctx.drawTextWithShadow(textRenderer, Text.literal(s.name), rx + 8, ry2, C_TEXT);

                    String valStr = (s.step >= 1f) ? String.valueOf((int) s.value) : String.format("%.1f", s.value);
                    int vw = textRenderer.getWidth(valStr);
                    ctx.drawTextWithShadow(textRenderer, Text.literal(valStr), rx + RIGHT_W - 8 - vw, ry2, C_ACCENT);

                    int barY = ry2 + 14;
                    int barX = rx + 8;
                    int barW = RIGHT_W - 16;
                    int barH = 3;

                    ctx.fill(barX, barY, barX + barW, barY + barH, C_SLIDER_BG);

                    float pct = (s.value - s.min) / (s.max - s.min);
                    int fillW = (int) (barW * pct);
                    ctx.fill(barX, barY, barX + fillW, barY + barH, C_ACCENT);

                    int kx = barX + fillW;
                    int ky = barY + barH / 2;
                    ctx.fill(kx - 4, ky - 4, kx + 4, ky + 4, C_ACCENT);
                    ctx.fill(kx - 2, ky - 2, kx + 2, ky + 2, 0xFFFFFFFF);

                    ry2 += 32;
                }
            }
        } else {
            ctx.drawTextWithShadow(textRenderer, Text.literal("Выбери модуль"),
                    rx + 8, y + 40, C_TEXT_DIM);
            ctx.drawTextWithShadow(textRenderer, Text.literal("для настройки"),
                    rx + 8, y + 52, C_TEXT_DIM);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = (int) guiX;
        int y = (int) guiY;

        // Sidebar categories
        for (int i = 0; i < CATS.length; i++) {
            int cy2 = y + 46 + i * 20;
            if (mx >= x && mx <= x + SIDEBAR_W && my >= cy2 && my <= cy2 + 18) {
                selectedCategory = i;
                selectedModule = null;
                return true;
            }
        }

        // Middle module rows
        int mx2 = x + SIDEBAR_W + 1;
        List<Module> mods = ModuleManager.byCategory(CATS[selectedCategory]);
        for (int i = 0; i < mods.size(); i++) {
            int ry = y + 36 + i * 20;
            if (ry + 18 > y + H - 4) break;

            if (mx >= mx2 && mx <= mx2 + MIDDLE_W && my >= ry && my <= ry + 18) {
                Module m = mods.get(i);

                // Click on toggle area (right side)
                int tx = mx2 + MIDDLE_W - 12;
                if (mx >= tx - 8 && mx <= tx + 8) {
                    m.toggle();
                    return true;
                }

                // Click on row → select module
                if (!m.settings.isEmpty()) {
                    selectedModule = m;
                } else {
                    m.toggle();
                }
                return true;
            }
        }

        // Right panel — sliders and toggles
        int rx = mx2 + MIDDLE_W + 1;
        if (selectedModule != null && !selectedModule.settings.isEmpty()) {
            int ry2 = y + 28;
            for (Module.Setting s : selectedModule.settings) {
                if (ry2 + 24 > y + H - 4) break;

                if (s.isBool) {
                    int tx = rx + RIGHT_W - 12;
                    int ty = ry2 + 9;
                    if (mx >= tx - 8 && mx <= tx + 8 && my >= ty - 8 && my <= ty + 8) {
                        s.inc();
                        return true;
                    }
                    ry2 += 24;
                } else {
                    int barX = rx + 8;
                    int barW = RIGHT_W - 16;
                    int barY = ry2 + 14;
                    if (my >= barY - 6 && my <= barY + 6 && mx >= barX && mx <= barX + barW) {
                        // Click or start drag on slider
                        float pct = (float)(mx - barX) / barW;
                        pct = Math.max(0f, Math.min(1f, pct));
                        s.value = s.min + pct * (s.max - s.min);
                        s.value = Math.round(s.value / s.step) * s.step;
                        dragSlider = s;
                        dragSliderX = barX;
                        dragSliderW = barW;
                        return true;
                    }
                    ry2 += 32;
                }
            }
        }

        // Drag window by header
        if (mx >= x && mx <= x + W && my >= y && my <= y + 20 && button == 0) {
            dragging = true;
            dragOffX = mx - guiX;
            dragOffY = my - guiY;
            return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragSlider != null) {
            float pct = (float)(mx - dragSliderX) / dragSliderW;
            pct = Math.max(0f, Math.min(1f, pct));
            dragSlider.value = dragSlider.min + pct * (dragSlider.max - dragSlider.min);
            dragSlider.value = Math.round(dragSlider.value / dragSlider.step) * dragSlider.step;
            return true;
        }

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
        dragSlider = null;
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
