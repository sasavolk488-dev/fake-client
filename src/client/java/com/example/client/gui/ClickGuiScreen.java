package com.example.client.gui;

import com.example.client.module.Module;
import com.example.client.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ClickGuiScreen extends Screen {

    private static final int W = 400;
    private static final int H = 250;
    private static final int SIDEBAR_W = 90;
    private static final int MIDDLE_W = 140;
    private static final int RIGHT_W = W - SIDEBAR_W - MIDDLE_W - 2;

    private static final int C_BG          = 0xF00D0D14;
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
    private static final int C_BIND_BG     = 0xFF1F1F2E;
    private static final int C_BIND_HOVER  = 0xFF2A2A40;
    private static final int C_BIND_ACTIVE = 0xFF3BB8F0;

    private float guiX = 0;
    private float guiY = 0;

    private int selectedCategory = 0;

    // Категории на русском
    private static final String[] CATS = {
            "Бой", "Движение", "Визуал", "Игрок", "Утилиты", "Темы", "Конфиги"
    };

    private Module selectedModule = null;
    private Module bindingModule = null;
    private boolean dragging = false;
    private double dragOffX, dragOffY;
    private long openTime;

    private Module.Setting dragSlider = null;
    private int dragSliderX = 0, dragSliderW = 0;

    private int rightScroll = 0;

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

        if (bindingModule != null) drawBindOverlay(ctx);

        ctx.getMatrices().pop();
    }

    private void drawWindow(DrawContext ctx, int mouseX, int mouseY) {
        int x = (int) guiX;
        int y = (int) guiY;

        ctx.fill(x + 4, y + 5, x + W + 4, y + H + 5, 0x80000000);
        ctx.fill(x, y, x + W, y + H, C_BG);

        // === SIDEBAR ===
        ctx.fill(x, y, x + SIDEBAR_W, y + H, C_SIDEBAR);
        ctx.fill(x + SIDEBAR_W, y, x + SIDEBAR_W + 1, y + H, C_BORDER);

        int lx = x + 6, ly = y + 6;
        ctx.fill(lx, ly, lx + 18, ly + 18, C_ACCENT_DIM);
        ctx.drawTextWithShadow(textRenderer, Text.literal("L"), lx + 6, ly + 6, C_ACCENT);

        ctx.drawTextWithShadow(textRenderer, Text.literal("Liminar"), x + 30, y + 6, C_TEXT);
        ctx.drawTextWithShadow(textRenderer, Text.literal("1.21.4"), x + 30, y + 16, C_TEXT);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Free"), x + 30, y + 24, C_ACCENT);

        ctx.fill(x + 6, y + 40, x + SIDEBAR_W - 6, y + 41, C_BORDER);

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

        // Поиск
        int sy = y + H - 22;
        ctx.fill(x + 4, sy, x + SIDEBAR_W - 4, sy + 16, C_SEARCH_BG);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Поиск..."), x + 10, sy + 4, C_TEXT_DIM);

        // === MIDDLE (список модулей) ===
        int mx = x + SIDEBAR_W + 1;
        ctx.fill(mx, y, mx + MIDDLE_W, y + H, C_MIDDLE);

        ctx.drawTextWithShadow(textRenderer, Text.literal(CATS[selectedCategory]), mx + 8, y + 7, C_TEXT);

        List<Module> mods = ModuleManager.byCategory(getCategoryKey(selectedCategory));
        int enabledCount = 0;
        for (Module m : mods) if (m.enabled) enabledCount++;
        ctx.drawTextWithShadow(textRenderer, Text.literal(enabledCount + "/" + mods.size() + " вкл"),
                mx + 8, y + 18, C_TEXT_DIM);

        ctx.fill(mx, y + 32, mx + MIDDLE_W, y + 33, C_BORDER);

        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int ry = y + 36 + i * 22;
            if (ry + 20 > y + H - 4) break;

            boolean hover = mouseX >= mx && mouseX <= mx + MIDDLE_W && mouseY >= ry && mouseY <= ry + 20;
            boolean isSelected = (selectedModule == m);

            if (isSelected) ctx.fill(mx, ry, mx + MIDDLE_W, ry + 20, C_ROW_ACTIVE);
            else if (hover) ctx.fill(mx, ry, mx + MIDDLE_W, ry + 20, C_ROW_HOVER);

            int tc = m.enabled ? C_TEXT : C_TEXT_DIM;
            ctx.drawTextWithShadow(textRenderer, Text.literal(m.name), mx + 8, ry + 6, tc);

            // Кнопка бинда
            int bindW = 22, bindH = 12;
            int bindX = mx + MIDDLE_W - 22 - bindW - 6;
            int bindY = ry + 4;

            boolean bindHover = mouseX >= bindX && mouseX <= bindX + bindW
                    && mouseY >= bindY && mouseY <= bindY + bindH;

            String bindText = m.bindKey > 0 ? getKeyName(m.bindKey) : "-";
            int bindBg = m.bindKey > 0 ? C_BIND_ACTIVE : (bindHover ? C_BIND_HOVER : C_BIND_BG);

            ctx.fill(bindX, bindY, bindX + bindW, bindY + bindH, bindBg);

            int bindTextColor = m.bindKey > 0 ? 0xFF000000 : C_TEXT_DIM;
            int tw = textRenderer.getWidth(bindText);
            ctx.drawTextWithShadow(textRenderer, Text.literal(bindText),
                    bindX + (bindW - tw) / 2, bindY + 2, bindTextColor);

            // Тумблер
            int tr = 5;
            int tx = mx + MIDDLE_W - 12;
            int ty = ry + 10;

            ctx.fill(tx - tr, ty - tr, tx + tr, ty + tr, m.enabled ? C_ACCENT : C_TOGGLE_OFF);

            if (m.enabled) {
                ctx.fill(tx - 2, ty, tx - 1, ty + 2, 0xFFFFFFFF);
                ctx.fill(tx - 1, ty + 1, tx, ty + 3, 0xFFFFFFFF);
                ctx.fill(tx, ty, tx + 1, ty + 2, 0xFFFFFFFF);
                ctx.fill(tx + 1, ty - 1, tx + 2, ty + 1, 0xFFFFFFFF);
            }
        }

        // === RIGHT (настройки) ===
        int rx = mx + MIDDLE_W + 1;
        ctx.fill(rx, y, rx + RIGHT_W, y + H, C_RIGHT);
        ctx.fill(rx - 1, y, rx, y + H, C_BORDER);

        ctx.drawTextWithShadow(textRenderer, Text.literal("Настройки"), rx + 8, y + 7, C_HEADER);
        ctx.fill(rx, y + 20, rx + RIGHT_W, y + 21, C_BORDER);

        if (selectedModule != null && !selectedModule.settings.isEmpty()) {
            int panelTop = y + 24;
            int panelBottom = y + H - 4;
            int panelHeight = panelBottom - panelTop;

            int contentHeight = 0;
            for (Module.Setting s : selectedModule.settings) {
                contentHeight += s.isBool ? 24 : 32;
            }

            int maxScroll = Math.max(0, contentHeight - panelHeight);
            if (rightScroll > maxScroll) rightScroll = maxScroll;
            if (rightScroll < 0) rightScroll = 0;

            ctx.enableScissor(rx, panelTop, rx + RIGHT_W, panelBottom);

            int ry2 = panelTop - rightScroll;
            for (Module.Setting s : selectedModule.settings) {
                if (s.isBool) {
                    if (ry2 + 24 >= panelTop && ry2 <= panelBottom) {
                        String localized = translateSetting(s.name);
                        ctx.drawTextWithShadow(textRenderer, Text.literal(localized), rx + 8, ry2 + 4, C_TEXT);

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
                    }
                    ry2 += 24;
                } else {
                    if (ry2 + 32 >= panelTop && ry2 <= panelBottom) {
                        String localized = translateSetting(s.name);
                        ctx.drawTextWithShadow(textRenderer, Text.literal(localized), rx + 8, ry2, C_TEXT);

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
                    }
                    ry2 += 32;
                }
            }

            ctx.disableScissor();

            // Полоса скролла
            if (maxScroll > 0) {
                int sbX = rx + RIGHT_W - 3;
                int sbY = panelTop;
                int sbH = panelHeight;
                int thumbH = Math.max(20, sbH * panelHeight / contentHeight);
                int thumbY = sbY + (int) ((float) rightScroll / maxScroll * (sbH - thumbH));

                ctx.fill(sbX, sbY, sbX + 2, sbY + sbH, 0xFF1A1A28);
                ctx.fill(sbX, thumbY, sbX + 2, thumbY + thumbH, C_ACCENT);
            }
        } else {
            ctx.drawTextWithShadow(textRenderer, Text.literal("Выбери модуль"),
                    rx + 8, y + 40, C_TEXT_DIM);
            ctx.drawTextWithShadow(textRenderer, Text.literal("для настройки"),
                    rx + 8, y + 52, C_TEXT_DIM);
        }
    }

    private void drawBindOverlay(DrawContext ctx) {
        ctx.fill(0, 0, width, height, 0xA0000000);

        int bw = 220, bh = 70;
        int bx = (width - bw) / 2;
        int by = (height - bh) / 2;

        ctx.fill(bx + 3, by + 3, bx + bw + 3, by + bh + 3, 0x60000000);
        ctx.fill(bx, by, bx + bw, by + bh, 0xF0141418);
        ctx.fill(bx, by, bx + bw, by + 2, C_ACCENT);

        ctx.drawTextWithShadow(textRenderer, Text.literal("Нажми клавишу..."),
                bx + 12, by + 16, C_TEXT);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Модуль: " + bindingModule.name),
                bx + 12, by + 32, C_ACCENT);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Esc — отмена"),
                bx + 12, by + 50, C_TEXT_DIM);
    }

    /**
     * Русская категория → английский ключ для ModuleManager
     */
    private String getCategoryKey(int index) {
        switch (index) {
            case 0: return "Combat";
            case 1: return "Movement";
            case 2: return "Render";
            case 3: return "Player";
            case 4: return "Utilities";
            case 5: return "Themes";
            case 6: return "Configs";
            default: return "Combat";
        }
    }

    /**
     * Перевод названий настроек модулей
     */
    private String translateSetting(String name) {
        switch (name) {
            // TriggerBot
            case "Range": return "Дистанция";
            case "Min Delay": return "Мин. задержка";
            case "Max Delay": return "Макс. задержка";
            case "Randomize": return "Рандомизация";
            case "Crit Only": return "Только криты";
            case "Smart Crits": return "Умные криты";
            case "Early Mace": return "Ранний удар";
            case "Through Blocks": return "Через блоки";
            case "Through Players": return "Через игроков";
            case "Hit While Eating": return "Бить когда ешь";
            case "Weapon Only": return "Только оружием";
            case "Sprint Reset": return "Сброс спринта";
            case "Players": return "Игроки";
            case "No Armor": return "Без брони";
            case "Mobs": return "Мобы";
            case "Animals": return "Животные";
            case "Friends": return "Друзья";
            case "Invisible": return "Невидимые";
            case "No GUI": return "Не бить в GUI";
            case "Focus One": return "Фокус на одном";
            case "Whitelist": return "Белый список";

            // KillAura
            case "Use Rotation": return "Использовать Rotation";
            case "Walls": return "Через стены";
            case "Min CPS": return "Мин. CPS";
            case "Max CPS": return "Макс. CPS";

            // Rotation
            case "Speed": return "Скорость";
            case "Smooth": return "Плавность";
            case "Jitter": return "Дрожание";
            case "Mode": return "Режим";
            case "Reaction": return "Реакция";

            default: return name;
        }
    }

    private String getKeyName(int key) {
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name != null) return name.toUpperCase();
        switch (key) {
            case GLFW.GLFW_KEY_SPACE: return "ПРОБЕЛ";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "ЛSH";
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "ПSH";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "ЛCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "ПCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT: return "ЛALT";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "ПALT";
            case GLFW.GLFW_KEY_TAB: return "TAB";
            case GLFW.GLFW_KEY_ENTER: return "ENT";
            default: return "K" + key;
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount, double vertical) {
        int x = (int) guiX;
        int y = (int) guiY;
        int mx2 = x + SIDEBAR_W + 1;
        int rx = mx2 + MIDDLE_W + 1;

        if (selectedModule != null && mx >= rx && mx <= rx + RIGHT_W
                && my >= y + 24 && my <= y + H - 4) {
            rightScroll -= (int)(vertical * 15);
            if (rightScroll < 0) rightScroll = 0;
            return true;
        }

        return super.mouseScrolled(mx, my, amount, vertical);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (bindingModule != null) {
            bindingModule = null;
            return true;
        }

        if (selectedModule != null && button == 0) {
            int x = (int) guiX;
            int y = (int) guiY;
            int mx2 = x + SIDEBAR_W + 1;
            int rx = mx2 + MIDDLE_W + 1;

            int panelTop = y + 24;
            int panelBottom = y + H - 4;

            if (mx >= rx && mx <= rx + RIGHT_W && my >= panelTop && my <= panelBottom) {
                int ry2 = panelTop - rightScroll;
                for (Module.Setting s : selectedModule.settings) {
                    if (s.isBool) {
                        if (ry2 + 24 >= panelTop && ry2 <= panelBottom) {
                            int tx = rx + RIGHT_W - 12;
                            int ty = ry2 + 9;
                            if (mx >= tx - 8 && mx <= tx + 8 && my >= ty - 8 && my <= ty + 8) {
                                s.inc();
                                return true;
                            }
                        }
                        ry2 += 24;
                    } else {
                        if (ry2 + 32 >= panelTop && ry2 <= panelBottom) {
                            int barX = rx + 8;
                            int barW = RIGHT_W - 16;
                            int barY = ry2 + 14;
                            if (my >= barY - 6 && my <= barY + 6 && mx >= barX && mx <= barX + barW) {
                                float pct = (float)(mx - barX) / barW;
                                pct = Math.max(0f, Math.min(1f, pct));
                                s.value = s.min + pct * (s.max - s.min);
                                s.value = Math.round(s.value / s.step) * s.step;
                                dragSlider = s;
                                dragSliderX = barX;
                                dragSliderW = barW;
                                return true;
                            }
                        }
                        ry2 += 32;
                    }
                }
            }
        }

        int x = (int) guiX;
        int y = (int) guiY;

        for (int i = 0; i < CATS.length; i++) {
            int cy2 = y + 46 + i * 20;
            if (mx >= x && mx <= x + SIDEBAR_W && my >= cy2 && my <= cy2 + 18) {
                selectedCategory = i;
                selectedModule = null;
                rightScroll = 0;
                return true;
            }
        }

        int mx2 = x + SIDEBAR_W + 1;
        List<Module> mods = ModuleManager.byCategory(getCategoryKey(selectedCategory));
        for (int i = 0; i < mods.size(); i++) {
            int ry = y + 36 + i * 22;
            if (ry + 20 > y + H - 4) break;

            if (mx >= mx2 && mx <= mx2 + MIDDLE_W && my >= ry && my <= ry + 20) {
                Module m = mods.get(i);

                int bindW = 22, bindH = 12;
                int bindX = mx2 + MIDDLE_W - 22 - bindW - 6;
                int bindY = ry + 4;

                if (mx >= bindX && mx <= bindX + bindW && my >= bindY && my <= bindY + bindH) {
                    if (button == 1) m.bindKey = -1;
                    else bindingModule = m;
                    return true;
                }

                int tx = mx2 + MIDDLE_W - 12;
                if (mx >= tx - 8 && mx <= tx + 8) {
                    m.toggle();
                    return true;
                }

                if (!m.settings.isEmpty()) {
                    selectedModule = m;
                    rightScroll = 0;
                } else {
                    m.toggle();
                }
                return true;
            }
        }

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
        if (bindingModule != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                bindingModule = null;
                return true;
            }
            bindingModule.bindKey = keyCode;
            bindingModule = null;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    }
