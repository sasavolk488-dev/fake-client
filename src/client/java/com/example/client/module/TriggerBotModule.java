package com.example.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.Random;

public class TriggerBotModule extends Module {

    public final Setting minDelaySet;
    public final Setting maxDelaySet;
    public final Setting rangeSet;
    public final Setting playersOnlySet;
    public final Setting critOnlySet;
    public final Setting weaponOnlySet;

    private long lastAttack = 0;
    private long targetSeenAt = 0;
    private final Random random = new Random();

    public TriggerBotModule() {
        super("TriggerBot", "Атака при наведении", "Combat", false);
        minDelaySet = num("Min Delay", 60f, 10f, 500f, 5f);
        maxDelaySet = num("Max Delay", 120f, 10f, 500f, 5f);
        rangeSet = num("Range", 3.0f, 1.0f, 6.0f, 0.1f);
        playersOnlySet = bool("Players Only", false);
        critOnlySet = bool("Crit Only", false);
        weaponOnlySet = bool("Weapon Only", true);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        if (weaponOnlySet.boolValue) {
            if (!isWeapon(mc)) {
                targetSeenAt = 0;
                return;
            }
        }

        HitResult hit = mc.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.ENTITY) {
            targetSeenAt = 0;
            return;
        }

        Entity target = ((EntityHitResult) hit).getEntity();

        if (!(target instanceof LivingEntity)) {
            targetSeenAt = 0;
            return;
        }
        if (target == mc.player) return;
        if (!target.isAlive()) return;

        if (playersOnlySet.boolValue && !(target instanceof PlayerEntity)) {
            targetSeenAt = 0;
            return;
        }

        double dist = mc.player.distanceTo(target);
        if (dist > rangeSet.value) {
            targetSeenAt = 0;
            return;
        }

        if (critOnlySet.boolValue) {
            if (mc.player.isOnGround() || mc.player.isTouchingWater() ||
                mc.player.isClimbing() || mc.player.hasStatusEffect(
                    net.minecraft.entity.effect.StatusEffects.BLINDNESS)) {
                return;
            }
            if (mc.player.getVehicle() != null) return;
        }

        long now = System.currentTimeMillis();
        if (targetSeenAt == 0) {
            targetSeenAt = now;
            return;
        }

        int minDelay = (int) minDelaySet.value;
        int maxDelay = (int) maxDelaySet.value;
        int delay = minDelay + random.nextInt(Math.max(1, maxDelay - minDelay + 1));

        if (now - targetSeenAt < delay) return;
        if (now - lastAttack < delay) return;

        if (mc.getNetworkHandler() != null) {
            PlayerInteractEntityC2SPacket packet = PlayerInteractEntityC2SPacket.attack(
                    target, mc.player.isSneaking());
            mc.getNetworkHandler().sendPacket(packet);
            mc.player.swingHand(Hand.MAIN_HAND);
            lastAttack = now;
            targetSeenAt = now;
        }
    }

    private boolean isWeapon(MinecraftClient mc) {
        var item = mc.player.getMainHandStack().getItem();
        return item instanceof net.minecraft.item.SwordItem
            || item instanceof net.minecraft.item.AxeItem
            || item instanceof net.minecraft.item.TridentItem;
    }

    @Override
    public void toggle() {
        super.toggle();
        targetSeenAt = 0;
        lastAttack = 0;
    }
          }
