package com.example.client;

import com.example.client.gui.ClickGuiScreen;
import com.example.client.module.Module;
import com.example.client.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class Fake_clientClient implements ClientModInitializer {

    private static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fake_client.clickgui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.fake_client"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Открытие GUI
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new ClickGuiScreen());
                }
            }

            // Проверка биндов модулей
            if (client.currentScreen == null) {
                long handle = client.getWindow().getHandle();
                for (Module m : ModuleManager.modules) {
                    if (m.bindKey > 0) {
                        // Проверяем нажатие через GLFW
                        if (GLFW.glfwGetKey(handle, m.bindKey) == GLFW.GLFW_PRESS) {
                            // Анти-спам: проверяем что не нажато на прошлом тике
                            if (!m.wasPressed) {
                                m.toggle();
                                m.wasPressed = true;
                            }
                        } else {
                            m.wasPressed = false;
                        }
                    }
                }
            }

            ModuleManager.onTick();
        });
    }
}
