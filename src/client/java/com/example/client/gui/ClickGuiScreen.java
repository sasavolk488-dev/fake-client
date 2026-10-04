package com.example.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ClickGuiScreen extends Screen {

    private int guiX = 40;
    private int guiY = 40;

    public ClickGuiScreen() {
        super(Text.literal("ClickGUI"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x40000000);
        ctx.fill(guiX, guiY, guiX + 320, guiY + 240, 0xE6121216);
        ctx.fill(guiX, guiY, guiX + 320, guiY + 24, 0xFF19191E);
        ctx.drawTextWithShadow(textRenderer, Text.literal("ClickGUI"), guiX + 8, guiY + 8, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Combat"), guiX + 8, guiY + 40, 0xFF44AAFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("KillAura"), guiX + 16, guiY + 60, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("AutoClicker"), guiX + 16, guiY + 80, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Reach"), guiX + 16, guiY + 100, 0xFFA0A0A0);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
                               }

