package com.example.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {

    public static final List<Module> modules = new ArrayList<>();

    public static void init() {
        modules.clear();

        modules.add(new Module("KillAura", "Автонаведение", "Combat", false));
        modules.add(new Module("AutoClicker", "Автокликер", "Combat", false));
        modules.add(new Module("Reach", "Дистанция", "Combat", false));
        modules.add(new Module("Velocity", "Анти-отброс", "Combat", false));
        modules.add(new Module("Criticals", "Криты", "Combat", false));

        modules.add(new AutoSprintModule());
        modules.add(new FlyModule());
        modules.add(new SpeedModule());
        modules.add(new NoFallModule());

        modules.add(new EspModule());
        modules.add(new FullbrightModule());
        modules.add(new Module("Tracers", "Линии", "Render", false));
        modules.add(new Module("Xray", "Прозрачные блоки", "Render", false));

        modules.add(new Module("Hud", "HUD", "Misc", true));
        modules.add(new Module("AntiAFK", "Анти-AFK", "Misc", false));
        modules.add(new Module("NameProtect", "Скрыть ник", "Misc", false));
    }

    public static void onTick() {
        for (Module m : modules) {
            if (m.enabled) {
                try {
                    if (m instanceof AutoSprintModule) ((AutoSprintModule) m).onTick();
                    if (m instanceof FlyModule) ((FlyModule) m).onTick();
                    if (m instanceof SpeedModule) ((SpeedModule) m).onTick();
                    if (m instanceof NoFallModule) ((NoFallModule) m).onTick();
                    if (m instanceof EspModule) ((EspModule) m).onTick();
                    if (m instanceof FullbrightModule) ((FullbrightModule) m).onTick();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public static List<Module> byCategory(String cat) {
        List<Module> list = new ArrayList<>();
        for (Module m : modules) if (m.category.equals(cat)) list.add(m);
        return list;
    }

    public static class AutoSprintModule extends Module {
        public AutoSprintModule() {
            super("AutoSprint", "Автоспринт", "Movement", true);
        }
        public void onTick() {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;
            boolean fwd = mc.options.forwardKey.isPressed();
            boolean food = mc.player.getHungerManager().getFoodLevel() > 6;
            if (fwd && food && !mc.player.isSneaking() && !mc.player.isUsingItem()) {
                mc.player.setSprinting(true);
            }
        }
    }

    public static class EspModule extends Module {
        public EspModule() {
            super("ESP", "Подсветка игроков", "Render", true);
        }
        public void onTick() {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world == null) return;
            for (Entity e : mc.world.getEntities()) {
                if (e instanceof PlayerEntity p && p != mc.player) {
                    p.setGlowing(enabled);
                }
            }
        }
    }

    public static class FullbrightModule extends Module {
        public FullbrightModule() {
            super("Fullbright", "Яркость", "Render", true);
        }
        public void onTick() {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.options.getGamma().setValue(100.0);
        }
    }

    public static class NoFallModule extends Module {
        public NoFallModule() {
            super("NoFall", "Без урона", "Movement", false);
        }
        public void onTick() {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;
            mc.player.fallDistance = 0f;
        }
    }

    public static class SpeedModule extends Module {
        public SpeedModule() {
            super("Speed", "Ускорение", "Movement", false);
        }
        public void onTick() {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;
            mc.player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SPEED, 40, 1, false, false));
        }
    }

    public static class FlyModule extends Module {
        public FlyModule() {
            super("Fly", "Полёт", "Movement", false);
        }
        public void onTick() {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;
            if (enabled) {
                mc.player.getAbilities().flying = true;
                mc.player.getAbilities().allowFlying = true;
            } else {
                if (!mc.player.isCreative()) {
                    mc.player.getAbilities().flying = false;
                    mc.player.getAbilities().allowFlying = false;
                }
            }
        }
    }
                    }
