package com.example.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

public class RotationModule extends Module {

    public final Setting speedSet;
    public final Setting smoothSet;
    public final Setting randomizeSet;
    public final Setting jitterSet;
    public final Setting modeSet;
    public final Setting reactionSet;

    private final Random random = new Random();
    private float serverYaw = 0;
    private float serverPitch = 0;

    private float lastTargetYaw = 0;
    private float lastTargetPitch = 0;

    // === ANTI-ROBOT STATE ===
    private long lastReaction = 0;
    private float microJitterYaw = 0;
    private float microJitterPitch = 0;
    private long lastMicroJitter = 0;

    public RotationModule() {
        super("Rotation", "Плавный поворот головы", "Combat", false);
        speedSet = num("Speed", 30f, 5f, 180f, 5f);
        smoothSet = num("Smooth", 0.45f, 0.05f, 1f, 0.05f);
        randomizeSet = num("Randomize", 1.5f, 0f, 8f, 0.5f);
        jitterSet = num("Jitter", 0.15f, 0f, 2f, 0.05f);
        modeSet = num("Mode", 0f, 0f, 2f, 1f);
        reactionSet = num("Reaction", 80f, 0f, 400f, 10f);
    }

    public float getYaw() { return serverYaw; }
    public float getPitch() { return serverPitch; }

    public void setTarget(Entity target) {
        if (target == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        double dx = target.getX() - mc.player.getX();
        double dz = target.getZ() - mc.player.getZ();

        // цель — грудь, а не глаза (реалистичнее)
        double targetY = target.getY() + target.getHeight() * 0.5;
        double selfY = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose());
        double dy = targetY - selfY;

        double distXZ = Math.sqrt(dx * dx + dz * dz);

        lastTargetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        lastTargetPitch = (float) -Math.toDegrees(Math.atan2(dy, distXZ));
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        float targetYaw = lastTargetYaw;
        float targetPitch = lastTargetPitch;

        float speed = speedSet.value;
        float smooth = smoothSet.value;
        float randomAmt = randomizeSet.value;
        float jitter = jitterSet.value;
        int mode = (int) modeSet.value;
        float reaction = reactionSet.value;

        // === ЗАДЕРЖКА РЕАКЦИИ ===
        // человек не реагирует мгновенно — 80-400мс
        long now = System.currentTimeMillis();
        if (reaction > 0 && now - lastReaction < reaction) {
            // во время задержки — смотрим как обычно
            serverYaw = mc.player.getYaw();
            serverPitch = mc.player.getPitch();
            return;
        }
        lastReaction = now;

        // === МИКРО-ДЖИТТЕР ===
        // каждые 100-300мс добавляем микро-колебание — "живая рука"
        if (now - lastMicroJitter > 100 + random.nextInt(200)) {
            microJitterYaw = (random.nextFloat() - 0.5f) * 0.8f;
            microJitterPitch = (random.nextFloat() - 0.5f) * 0.5f;
            lastMicroJitter = now;
        }

        if (randomAmt > 0.01f) {
            targetYaw += (random.nextFloat() - 0.5f) * randomAmt;
            targetPitch += (random.nextFloat() - 0.5f) * randomAmt * 0.5f;
        }

        float curYaw = mc.player.getYaw();
        float curPitch = mc.player.getPitch();

        float yawDiff = wrapDegrees(targetYaw - curYaw);
        float pitchDiff = wrapDegrees(targetPitch - curPitch);

        float newYaw;
        float newPitch;

        if (mode == 0) {
            // === LEGIT ===
            // нелинейная интерполяция: ускорение + замедление
            float absDiff = Math.abs(yawDiff);
            float easeFactor;

            if (absDiff > 30f) {
                // большая разница — быстрее
                easeFactor = smooth * 1.3f;
            } else if (absDiff > 10f) {
                // средняя — обычно
                easeFactor = smooth;
            } else {
                // маленькая — медленно, аккуратно
                easeFactor = smooth * 0.6f;
            }

            easeFactor = MathHelper.clamp(easeFactor, 0.05f, 1f);

            float easeYaw = MathHelper.clamp(yawDiff * easeFactor, -speed, speed);
            float easePitch = MathHelper.clamp(pitchDiff * easeFactor, -speed * 0.5f, speed * 0.5f);

            newYaw = curYaw + easeYaw;
            newPitch = curPitch + easePitch;

            // jitter — рука дрожит
            if (jitter > 0.01f) {
                newYaw += (random.nextFloat() - 0.5f) * jitter;
                newPitch += (random.nextFloat() - 0.5f) * jitter;
            }

            // микро-джиттер
            newYaw += microJitterYaw;
            newPitch += microJitterPitch;
        } else if (mode == 1) {
            // Fast
            float yawStep = Math.min(Math.abs(yawDiff), speed);
            float pitchStep = Math.min(Math.abs(pitchDiff), speed);
            newYaw = curYaw + Math.copySign(yawStep, yawDiff);
            newPitch = curPitch + Math.copySign(pitchStep, pitchDiff);
        } else {
            // Cinematic
            float ease = smooth * 0.4f;
            newYaw = curYaw + yawDiff * ease;
            newPitch = curPitch + pitchDiff * ease;
        }

        newPitch = MathHelper.clamp(newPitch, -90f, 90f);

        serverYaw = newYaw;
        serverPitch = newPitch;

        mc.player.setYaw(newYaw);
        mc.player.setPitch(newPitch);
        mc.player.prevYaw = newYaw;
        mc.player.prevPitch = newPitch;
    }

    private static float wrapDegrees(float angle) {
        angle = angle % 360.0f;
        if (angle >= 180.0f) angle -= 360.0f;
        if (angle < -180.0f) angle += 360.0f;
        return angle;
    }
                                                                 }
