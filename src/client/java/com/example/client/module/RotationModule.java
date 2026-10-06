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

    private final Random random = new Random();
    private float serverYaw = 0;
    private float serverPitch = 0;

    private float lastTargetYaw = 0;
    private float lastTargetPitch = 0;

    public RotationModule() {
        super("Rotation", "Плавный поворот головы", "Combat", false);
        speedSet = num("Speed", 45f, 5f, 180f, 5f);
        smoothSet = num("Smooth", 0.35f, 0.05f, 1f, 0.05f);
        randomizeSet = num("Randomize", 2.0f, 0f, 10f, 0.5f);
        jitterSet = num("Jitter", 0.3f, 0f, 3f, 0.1f);
        modeSet = num("Mode", 0f, 0f, 2f, 1f);
    }

    public float getYaw() { return serverYaw; }
    public float getPitch() { return serverPitch; }

    public void setTarget(Entity target) {
        if (target == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        double dx = target.getX() - mc.player.getX();
        double dz = target.getZ() - mc.player.getZ();

        double targetY = target.getY() + target.getHeight() * 0.6;
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
            float easeYaw = MathHelper.clamp(yawDiff * smooth, -speed, speed);
            float easePitch = MathHelper.clamp(pitchDiff * smooth, -speed * 0.6f, speed * 0.6f);

            newYaw = curYaw + easeYaw;
            newPitch = curPitch + easePitch;

            if (jitter > 0.01f) {
                newYaw += (random.nextFloat() - 0.5f) * jitter;
                newPitch += (random.nextFloat() - 0.5f) * jitter;
            }
        } else if (mode == 1) {
            float yawStep = Math.min(Math.abs(yawDiff), speed);
            float pitchStep = Math.min(Math.abs(pitchDiff), speed);
            newYaw = curYaw + Math.copySign(yawStep, yawDiff);
            newPitch = curPitch + Math.copySign(pitchStep, pitchDiff);
        } else {
            float ease = smooth * 0.5f;
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
