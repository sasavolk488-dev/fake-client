package com.example.client.module;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {

    public static final List<Module> modules = new ArrayList<>();

    public static void init() {
        modules.clear();
        modules.add(new Module("KillAura", "Автонаведение", "Combat", true));
        modules.add(new Module("AutoClicker", "Автокликер", "Combat", true));
        modules.add(new Module("Reach", "Дистанция", "Combat", false));
        modules.add(new Module("Velocity", "Анти-отброс", "Combat", true));
        modules.add(new Module("Criticals", "Криты", "Combat", false));
        modules.add(new Module("Fly", "Полёт", "Movement", false));
        modules.add(new Module("Speed", "Ускорение", "Movement", true));
        modules.add(new Module("NoFall", "Без урона", "Movement", false));
        modules.add(new Module("ESP", "Подсветка", "Render", true));
        modules.add(new Module("Tracers", "Линии", "Render", false));
        modules.add(new Module("Fullbright", "Яркость", "Render", true));
        modules.add(new Module("Xray", "Xray", "Render", false));
        modules.add(new Module("Hud", "HUD", "Misc", true));
        modules.add(new Module("AntiAFK", "Анти-AFK", "Misc", true));
        modules.add(new Module("NameProtect", "Скрыть ник", "Misc", false));
    }

    public static void onTick() {
        for (Module m : modules) {
            if (m.enabled) {
                try { m.onTick(); } catch (Throwable ignored) {}
            }
        }
    }

    public static List<Module> byCategory(String cat) {
        List<Module> list = new ArrayList<>();
        for (Module m : modules) if (m.category.equals(cat)) list.add(m);
        return list;
    }
}
