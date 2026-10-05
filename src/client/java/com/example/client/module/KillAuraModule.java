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
    public final Setting rotSpeedSet;
    public final Setting silentRotSet;
    public final Setting randomizeSet;
    public final Setting throughWallsSet;

    private long lastAttack = 0;
    private final Random random = new Random();

    // silent rotation — сервер видит, клиент нет
    private float serverYaw = 0;
    private float serverPitch = 0;

    public KillAuraModule() {
        super("KillAura", "Автоатака ближайшего игрока", "Combat", false);
        rangeSet = num("Range", 3.0f, 1.0f, 6.0f, 0.1f);
        minCpsSet = num("Min CPS", 8f, 4f, 20f, 1f);
        maxCpsSet = num("Max CPS", 14f, 4f, 20f, 1f);
        rotSpeedSet = num("Rot Speed", 35f, 5f, 180f, 5f);
        silentRotSet = bool("Silent Rot", true);
        randomizeSet = bool("Randomize", true);
        throughWallsSet = bool("Walls", false);
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
        boolean silent = silentRotSet.boolValue;
        boolean randomize = randomizeSet.boolValue;
        boolean walls = throughWallsSet.boolValue;

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

        // ===== РАСЧЁТ ЦЕЛЕВЫХ УГЛОВ =====
        double dx = target.getX() - mc.player.getX();
        double dz = target.getZ() - mc.player.getZ();

        // цель по центру туловища
        double targetY = target.getY() + target.getHeight() * 0.6;
        double selfY = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose());
        double dy = targetY - selfY;

        double distXZ = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, distXZ));

        // небольшой рандом на цель — чтобы не бить всегда в одну точку
        if (randomize) {
            targetYaw += (random.nextFloat() - 0.5f) * 3.0f;
            targetPitch += (random.nextFloat() - 0.5f) * 2.0f;
        }

        // ===== ПЛАВНЫЙ ПОВОРОТ (SILENT) =====
        float curServerYaw = silent ? serverYaw : mc.player.getYaw();
        float curServerPitch = silent ? serverPitch : mc.player.getPitch();

        float yawDiff = wrapDegrees(targetYaw - curServerYaw);
        float pitchDiff = wrapDegrees(targetPitch - curServerPitch);

        // скорость плавная: чем больше разница, тем быстрее догоняем
        float yawStep = Math.min(Math.abs(yawDiff), rotSpeed);
        float pitchStep = Math.min(Math.abs(pitchDiff), rotSpeed * 0.7f);

        float newServerYaw = curServerYaw + Math.copySign(yawStep, yawDiff);
        float newServerPitch = MathHelper.clamp(
                curServerPitch + Math.copySign(pitchStep, pitchDiff),
                -90f, 90f);

        if (silent) {
            serverYaw = newServerYaw;
            serverPitch = newServerPitch;

            // применяем к entity — сервер увидит поворот
            mc.player.setYaw(newServerYaw);
            mc.player.setPitch(newServerPitch);
            mc.player.prevYaw = newServerYaw;
            mc.player.prevPitch = newServerPitch;
        } else {
            // обычный поворот — видно и тебе и серверу
            mc.player.setYaw(newServerYaw);
            mc.player.setPitch(newServerPitch);
        }

        // ===== ПРОВЕРКА НАВЕДЕНИЯ =====
        float yawToTarget = Math.abs(wrapDegrees(targetYaw - newServerYaw));
        float pitchToTarget = Math.abs(wrapDegrees(targetPitch - newServerPitch));

        // не бьём, пока не навелись (30° — порог)
        if (yawToTarget > 30.0f || pitchToTarget > 30.0f) return;

        // ===== АТАКА =====
        long now = System.currentTimeMillis();
        int cps = minCps + random.nextInt(Math.max(1, maxCps - minCps + 1));
        // небольшой рандом в задержке — не идеально ровные удары
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

    @Override
    public void toggle() {
        super.toggle();
        // при выключении сбрасываем silent rotation
        if (!enabled) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                serverYaw = mc.player.getYaw();
                serverPitch = mc.player.getPitch();
            }
        }
    }

    private static float wrapDegrees(float angle) {
        angle = angle % 360.0f;
        if (angle >= 180.0f) angle -= 360.0f;
        if (angle < -180.0f) angle += 360.0f;
        return angle;
    }
        }
