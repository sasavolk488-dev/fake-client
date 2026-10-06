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
    public final Setting wallsSet;

    private long lastAttack = 0;
    private final Random random = new Random();

    public KillAuraModule() {
        super("KillAura", "Автоатака ближайшего игрока", "Combat", false);
        rangeSet = num("Range", 3.0f, 1.0f, 6.0f, 0.1f);
        minCpsSet = num("Min CPS", 8f, 4f, 20f, 1f);
        maxCpsSet = num("Max CPS", 14f, 4f, 20f, 1f);
        useRotationSet = bool("Use Rotation", true);
        wallsSet = bool("Walls", false);
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
        boolean walls = wallsSet.boolValue;

        // ===== ПОИСК ЦЕЛИ =====
        PlayerEntity target = null;
        double bestDist = range * range;

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player) continue;
            if (!e.isAlive()) continue;
            if (!(e instanceof PlayerEntity)) continue;

            if (!walls && !mc.player.canSee(e)) continue;

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
            rotation.setTarget(target);
            rotation.onTick();

            targetYaw = rotation.getYaw();
            targetPitch = rotation.getPitch();
        } else {
            // встроенный поворот
            double dx = target.getX() - mc.player.getX();
            double dz = target.getZ() - mc.player.getZ();

            double targetY = target.getY() + target.getHeight() * 0.5;
            double selfY = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose());
            double dy = targetY - selfY;

            double distXZ = Math.sqrt(dx * dx + dz * dz);

            float realTargetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            float realTargetPitch = (float) -Math.toDegrees(Math.atan2(dy, distXZ));

            float rotSpeed = 30f;
            float curYaw = mc.player.getYaw();
            float curPitch = mc.player.getPitch();

            float yawDiff = wrapDegrees(realTargetYaw - curYaw);
            float pitchDiff = wrapDegrees(realTargetPitch - curPitch);

            float step = Math.min(Math.abs(yawDiff), rotSpeed);
            float pstep = Math.min(Math.abs(pitchDiff), rotSpeed * 0.6f);

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

        // ===== LEGIT CPS =====
        long now = System.currentTimeMillis();

        int cps = minCps + random.nextInt(Math.max(1, maxCps - minCps + 1));
        long baseDelay = 1000L / Math.max(1, cps);

        // большой разброс (±40%)
        long randomOffset = (long)((random.nextDouble() - 0.5) * baseDelay * 0.8);

        // иногда "пауза" — человек смотрит, прицеливается
        if (random.nextInt(20) == 0) {
            randomOffset += 150 + random.nextInt(200);
        }

        // иногда "burst" — два быстрых удара
        if (random.nextInt(15) == 0) {
            randomOffset -= baseDelay / 2;
        }

        long delay = Math.max(40, baseDelay + randomOffset);
        if (now - lastAttack < delay) return;

        // ===== УДАР =====
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
