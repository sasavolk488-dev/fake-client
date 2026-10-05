package com.example.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;

import java.util.Random;

public class KillAuraModule extends Module {

    public final Setting rangeSet;
    public final Setting minCpsSet;
    public final Setting maxCpsSet;
    public final Setting rotSpeedSet;

    private long lastAttack = 0;
    private final Random random = new Random();

    public KillAuraModule() {
        super("KillAura", "Автоатака ближайшего игрока", "Combat", false);
        rangeSet = num("Range", 3.0f, 1.0f, 6.0f, 0.1f);
        minCpsSet = num("Min CPS", 8f, 4f, 20f, 1f);
        maxCpsSet = num("Max CPS", 14f, 4f, 20f, 1f);
        rotSpeedSet = num("Rot Speed", 25f, 5f, 90f, 5f);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        float range = rangeSet.value;
        int minCps = (int) minCpsSet.value;
        int maxCps = (int) maxCpsSet.value;
        float rotSpeed = rotSpeedSet.value;

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

        double dx = target.getX() - mc.player.getX();
        double dz = target.getZ() - mc.player.getZ();
        double dy = (target.getY() + target.getEyeHeight(target.getPose()))
                  - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));

        double distXZ = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, distXZ));

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float yawDiff = wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = wrapDegrees(targetPitch - currentPitch);

        float newYaw = currentYaw + clamp(yawDiff, -rotSpeed, rotSpeed);
        float newPitch = currentPitch + clamp(pitchDiff, -rotSpeed, rotSpeed);

        mc.player.setYaw(newYaw);
        mc.player.setPitch(newPitch);

        float yawToTarget = Math.abs(wrapDegrees(targetYaw - mc.player.getYaw()));
        float pitchToTarget = Math.abs(wrapDegrees(targetPitch - mc.player.getPitch()));

        if (yawToTarget > 30.0f || pitchToTarget > 30.0f) return;

        long now = System.currentTimeMillis();
        int cps = minCps + random.nextInt(Math.max(1, maxCps - minCps + 1));
        long delay = 1000L / Math.max(1, cps);
        if (now - lastAttack < delay) return;

        if (mc.getNetworkHandler() != null) {
            PlayerInteractEntityC2SPacket packet = PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking());
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

    private static float clamp(float value, float min, float max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
            }
