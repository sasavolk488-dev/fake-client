package com.example.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

public class KillAuraModule extends Module {

    // === НАСТРОЙКИ ===
    public final Setting rangeSet;
    public final Setting minCpsSet;
    public final Setting maxCpsSet;
    public final Setting useRotationSet;

    private long lastAttack = 0;
    private final Random random = new Random();

    private float serverYaw = 0;
    private float serverPitch = 0;

    public KillAuraModule() {
        super("KillAura", "Автоатака ближайшего игрока", "Combat", false);
        rangeSet = num("Range", 3.0f, 1.0f, 6.0f, 0.1f);
        minCpsSet = num("Min CPS", 8f, 4f, 20f, 1f);
        maxCpsSet = num("Max CPS", 14f, 4f, 20f, 1f);
        useRotationSet = bool("Use Rotation", true);
    }

    private RotationModule getRotation() {
        for (Module m : ModuleManager.modules) {
            if (m instanceof RotationModule && m.enabled) {
                return (RotationModule) m;
            }
        }
        return null;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        float range = rangeSet.value;
        int minCps = (int) minCpsSet.value;
        int maxCps = (int) maxCpsSet.value;
        boolean useRotation = useRotationSet.boolValue;

        // ===== ПОИСК ЦЕЛИ =====
        PlayerEntity target = null;
        double bestDist = range * range;

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player) continue;
            if (!e.isAlive()) continue;
            if (!(e instanceof PlayerEntity)) continue;

            double d = mc.player.squaredDistanceTo(e);
            if (d < bestDist) {
                bestDist = d;
                target = (PlayerEntity) e;
            }
        }

        if (target == null) return;

        RotationModule rotation = useRotation ? getRotation() : null;

        // ===== ПОВОРОТ =====
        float targetYaw, targetPitch;

        if (rotation != null) {
            // используем Rotation модуль
            rotation.setTarget(target);
            rotation.onTick();

            targetYaw = rotation.getYaw();
            targetPitch = rotation.getPitch();
        } else {
            // встроенный поворот (как раньше)
            double dx = target.getX() - mc.player.getX();
            double dz = target.getZ() - mc.player.getZ();

            double targetY = target.getY() + target.getHeight() * 0.6;
            double selfY = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose());
            double dy = targetY - selfY;

            double distXZ = Math.sqrt(dx * dx + dz * dz);

            targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            targetPitch = (float) -Math.toDegrees(Math.atan2(dy, distXZ));

            float rotSpeed = 35f;
            float curYaw = mc.player.getYaw();
            float curPitch = mc.player.getPitch();

            float yawDiff = wrapDegrees(targetYaw - curYaw);
            float pitchDiff = wrapDegrees(targetPitch - curPitch);

            float step = Math.min(Math.abs(yawDiff), rotSpeed);
            float pstep = Math.min(Math.abs(pitchDiff), rotSpeed * 0.7f);

            float newYaw = curYaw + Math.copySign(step, yawDiff);
            float newPitch = MathHelper.clamp(curPitch + Math.copySign(pstep, pitchDiff), -90f, 90f);

            mc.player.setYaw(newYaw);
            mc.player.setPitch(newPitch);
            mc.player.prevYaw = newYaw;
            mc.player.prevPitch = newPitch;

            targetYaw = newYaw;
            targetPitch = newPitch;
        }

        // ===== ПРОВЕРКА НАВЕДЕНИЯ =====
        float yawToTarget = Math.abs(wrapDegrees(targetYaw - mc.player.getYaw()));
        float pitchToTarget = Math.abs(targetPitch - mc.player.getPitch());

        if (yawToTarget > 25.0f || pitchToTarget > 25.0f) return;

        // ===== АТАКА =====
        long now = System.currentTimeMillis();
        int cps = minCps + random.nextInt(Math.max(1, maxCps - minCps + 1));
        long delay = 1000L / Math.max(1, cps) + (random.nextInt(15) - 7);
        if (now - lastAttack < delay) return;

        if (mc.getNetworkHandler() != null) {
            PlayerInteractEntityC2SPacket packet = PlayerInteractEntityC2SPacket.attack(
                    target, mc.player.isSneaking());
            mc.getNetworkHandler().sendPacket(packet);
            mc.player.swingHand(Hand.MAIN_HAND);
            lastAttack = now;
        }
    }

    private static float wrapDegrees(float angle) {
        angle = angle % 360.0f;
        if (angle >= 180.0f) angle -= 360.0f;
        if (angle < -180.0f) angle += 360.0f;
        return angle;
    }
                    }
