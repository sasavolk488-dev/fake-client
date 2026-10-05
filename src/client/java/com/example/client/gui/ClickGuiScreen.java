package com.example.client.gui;

import com.example.client.module.Module;
import com.example.client.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ClickGuiScreen extends Screen {

    // === ЦВЕТА (Liminar style) ===
    private static final int C_BG          = 0xF00A0A0F;
    private static final int C_SIDEBAR     = 0xFF0D0D14;
    private static final int C_PANEL       = 0xFF10101A;
    private static final int C_PANEL_HD    = 0xFF151520;
    private static final int C_BORDER      = 0xFF1E1E2E;
    private static final int C_ACCENT      = 0xFF4FB8F7;
    private static final int C_ACCENT_DARK = 0xFF1A4A6E;
    private static final int C_TEXT        = 0xFFFFFFFF;
    private static final int C_TEXT_DIM    = 0xFF8888A0;
    private static final int C_TEXT_HEADER = 0xFF6A6A8A;
    private static final int C_ROW         = 0xFF14141E;
    private static final int C_ROW_HOVER   = 0xFF1A1A28;
    private static final int C_TOGGLE_OFF  = 0xFF2A2A3A;
    private static final int C_SLIDER_BG   = 0xFF2A2A3A;

    // === РАЗМЕРЫ ===
    private static final int W = 400;
    private static final int H = 240;
    private static final int SIDEBAR_W = 88;
    private static final int MIDDLE_W = 155;
    private static final int RIGHT_W = W - SIDEBAR_W - MIDDLE_W - 3;

    private float guiX = 0;
    private float guiY = 0;

    private int selectedCategory = 0;
    private static final String[] CATS = {"Combat", "Movement", "Render", "Player", "Utilities", "Themes", "Configs"};

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

        // Тень
        ctx.fill(x + 5, y + 6, x + W + 5, y + H + 6, 0x80000000);

        // Основной фон
        ctx.fill(x, y, x + W, y + H, C_BG);

        // ============= SIDEBAR (левая) =============
        ctx.fill(x, y, x + SIDEBAR_W, y + H, C_SIDEBAR);
        ctx.fill(x + SIDEBAR_W - 1, y, x + SIDEBAR_W, y + H, C_BORDER);

        // Логотип
        int lx = x + 6, ly = y + 6;
        ctx.fill(lx, ly, lx + 20, ly + 20, C_ACCENT_DARK);
        ctx.fill(lx + 6, ly + 6, lx + 14, ly + 14, C_ACCENT);

        ctx.drawTextWithShadow(textRenderer, Text.literal("Liminar"), x + 30, y + 8, C_TEXT);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Free"), x + 30, y + 18, C_ACCENT);

        // Разделитель
        ctx.fill(x + 6, y + 32, x + SIDEBAR_W - 6, y + 33, C_BORDER);

        // Категории
        for (int i = 0; i < CATS.length; i++) {
            int cy2 = y + 40 + i * 24;
            boolean hover = mouseX >= x && mouseX <= x + SIDEBAR_W && mouseY >= cy2 && mouseY <= cy2 + 22;

            if (i == selectedCategory) {
                ctx.fill(x + 3, cy2, x + SIDEBAR_W - 3, cy2 + 22, C_PANEL_HD);
                ctx.fill(x + 3, cy2, x + 5, cy2 + 22, C_ACCENT);
            } else if (hover) {
                ctx.fill(x + 3, cy2, x + SIDEBAR_W - 3, cy2 + 22, C_ROW_HOVER);
            }

            // Иконка-точка
            int dotX = x + 14, dotY = cy2 + 8;
            int dotColor = i == selectedCategory ? C_ACCENT : C_TEXT_DIM;
            ctx.fill(dotX, dotY, dotX + 6, dotY + 6, dotColor);

            int textColor = i == selectedCategory ? C_TEXT : C_TEXT_DIM;
            ctx.drawTextWithShadow(textRenderer, Text.literal(CATS[i]),
                    x + 26, cy2 + 7, textColor);
        }

        // Поиск снизу
        int sy = y + H - 28;
        ctx.fill(x + 6, sy, x + SIDEBAR_W - 6, sy + 20, C_PANEL);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Поиск..."), x + 12, sy + 6, C_TEXT_DIM);

        // ============= MIDDLE PANEL (модули) =============
        int mx = x + SIDEBAR_W + 1;
        ctx.fill(mx, y, mx + MIDDLE_W, y + H, C_PANEL);

        // Заголовок категории
        ctx.drawTextWithShadow(textRenderer, Text.literal(CATS[selectedCategory]),
                mx + 8, y + 8, C_TEXT);

        // Счётчик включённых
        int total = 0, enabledCount = 0;
        List<Module> mods = ModuleManager.byCategory(CATS[selectedCategory]);
        for (Module m : mods) { total++; if (m.enabled) enabledCount++; }
        ctx.drawTextWithShadow(textRenderer, Text.literal(enabledCount + "/" + total + " вкл"),
                mx + 8, y + 20, C_TEXT_DIM);

        // Разделитель
        ctx.fill(mx, y + 34, mx + MIDDLE_W, y + 35, C_BORDER);

        // Список модулей
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int ry = y + 40 + i * 24;
            if (ry + 22 > y + H - 8) break;

            boolean hover = mouseX >= mx && mouseX <= mx + MIDDLE_W && mouseY >= ry && mouseY <= ry + 22;

            if (hover) {
                ctx.fill(mx, ry, mx + MIDDLE_W, ry + 22, C_ROW_HOVER);
            }

            // Название
            int textColor = m.enabled ? C_TEXT : C_TEXT_DIM;
            ctx.drawTextWithShadow(textRenderer, Text.literal(m.name), mx + 10, ry + 7, textColor);

            // Круглый тумблер справа (в стиле Liminar)
            int tr = 5; // радиус = 5, диаметр = 10
            int tx = mx + MIDDLE_W - 18;
            int ty = ry + 11;

            // Фон тумблера
            ctx.fill(tx - tr, ty - tr, tx + tr, ty + tr, m.enabled ? C_ACCENT : C_TOGGLE_OFF);

            // Галочка если включён
            if (m.enabled) {
                ctx.fill(tx - 2, ty - 1, tx - 1, ty + 1, 0xFFFFFFFF);
                ctx.fill(tx - 1, ty, tx, ty + 2, 0xFFFFFFFF);
                ctx.fill(tx, ty + 1, tx + 1, ty + 2, 0xFFFFFFFF);
                ctx.fill(tx + 1, ty, tx + 2, ty - 1, 0xFFFFFFFF);
            }
        }

        // ============= RIGHT PANEL (настройки) =============
        int rx = mx + MIDDLE_W + 1;
        ctx.fill(rx, y, rx + RIGHT_W, y + H, C_PANEL_HD);
        ctx.fill(rx - 1, y, rx, y + H, C_BORDER);

        // Заголовок
        ctx.drawTextWithShadow(textRenderer, Text.literal("Настройки"),
                rx + 8, y + 8, C_TEXT_HEADER);

        ctx.fill(rx, y + 22, rx + RIGHT_W, y + 23, C_BORDER);

        // Пример настроек (визуальные)
        drawSlider(ctx, rx + 8, y + 34, RIGHT_W - 16, "Дистанция", 30f, 3f, 30f);
        drawSlider(ctx, rx + 8, y + 66, RIGHT_W - 16, "Скорость", 25f, 5f, 90f);

        ctx.fill(rx, y + 98, rx + RIGHT_W, y + 99, C_BORDER);

        // Toggle-строки
        drawToggleRow(ctx, rx + 8, y + 108, RIGHT_W - 16, "Сохранение", true);
        drawToggleRow(ctx, rx + 8, y + 132, RIGHT_W - 16, "Авто-выкл", false);
        drawToggleRow(ctx, rx + 8, y + 156, RIGHT_W - 16, "Debug", false);

        ctx.fill(rx, y + 180, rx + RIGHT_W, y + 181, C_BORDER);

        ctx.drawTextWithShadow(textRenderer, Text.literal("Mace Helper"),
                rx + 8, y + 188, C_TEXT_HEADER);

        drawToggleRow(ctx, rx + 8, y + 202, RIGHT_W - 16, "Брать булаву", true);
    }

    private void drawSlider(DrawContext ctx, int x, int y, int w, String label, float value, float min, float max) {
        // Название + значение
        ctx.drawTextWithShadow(textRenderer, Text.literal(label), x, y, C_TEXT);

        String valStr = (value == (int) value) ? String.valueOf((int) value) : String.format("%.1f", value);
        int vw = textRenderer.getWidth(valStr);
        ctx.drawTextWithShadow(textRenderer, Text.literal(valStr), x + w - vw, y, C_ACCENT);

        // Полоса
        int barY = y + 14;
        int barH = 3;
        ctx.fill(x, barY, x + w, barY + barH, C_SLIDER_BG);

        float pct = (value - min) / (max - min);
        int fillW = (int) (w * pct);
        ctx.fill(x, barY, x + fillW, barY + barH, C_ACCENT);

        // Кружок
        int cx = x + fillW;
        int cy = barY + barH / 2;
        ctx.fill(cx - 4, cy - 4, cx + 4, cy + 4, C_ACCENT);
        ctx.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFFFFFFFF);
    }

    private void drawToggleRow(DrawContext ctx, int x, int y, int w, String label, boolean on) {
        ctx.drawTextWithShadow(textRenderer, Text.literal(label), x, y + 4, C_TEXT);

        int tr = 5;
        int tx = x + w - 8;
        int ty = y + 9;

        ctx.fill(tx - tr, ty - tr, tx + tr, ty + tr, on ? C_ACCENT : C_TOGGLE_OFF);

        if (on) {
            ctx.fill(tx - 2, ty - 1, tx - 1, ty + 1, 0xFFFFFFFF);
            ctx.fill(tx - 1, ty, tx, ty + 2, 0xFFFFFFFF);
            ctx.fill(tx, ty + 1, tx + 1, ty + 2, 0xFFFFFFFF);
            ctx.fill(tx + 1, ty, tx + 2, ty - 1, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = (int) guiX;
        int y = (int) guiY;

        // Сайдбар — категории
        for (int i = 0; i < CATS.length; i++) {
            int cy2 = y + 40 + i * 24;
            if (mx >= x && mx <= x + SIDEBAR_W && my >= cy2 && my <= cy2 + 22) {
                selectedCategory = i;
                return true;
            }
        }

        // Middle — модули
        int mx2 = x + SIDEBAR_W + 1;
        List<Module> mods = ModuleManager.byCategory(CATS[selectedCategory]);
        for (int i = 0; i < mods.size(); i++) {
            int ry = y + 40 + i * 24;
            if (ry + 22 > y + H - 8) break;
            if (mx >= mx2 && mx <= mx2 + MIDDLE_W && my >= ry && my <= ry + 22) {
                mods.get(i).toggle();
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
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
            }
