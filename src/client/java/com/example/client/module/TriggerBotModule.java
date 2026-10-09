package com.example.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
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
    public final Setting noWeaponSet;
    public final Setting critOnlySet;

    // Кого атаковать
    public final Setting targetPlayersSet;
    public final Setting targetNoArmorSet;
    public final Setting targetMobsSet;
    public final Setting targetAnimalsSet;
    public final Setting targetFriendsSet;
    public final Setting targetInvisibleSet;

    // Атака
    public final Setting throughWallsSet;

    private long lastAttack = 0;
    private long targetSeenAt = 0;
    private final Random random = new Random();

    public TriggerBotModule() {
        super("TriggerBot", "Атака при наведении", "Combat", false);
        minDelaySet = num("Min Delay", 60f, 10f, 500f, 5f);
        maxDelaySet = num("Max Delay", 120f, 10f, 500f, 5f);
        rangeSet = num("Range", 3.0f, 1.0f, 6.0f, 0.1f);
        noWeaponSet = bool("No Weapon", false);
        critOnlySet = bool("Crit Only", false);

        targetPlayersSet = bool("Players", true);
        targetNoArmorSet = bool("No Armor", false);
        targetMobsSet = bool("Mobs", false);
        targetAnimalsSet = bool("Animals", false);
        targetFriendsSet = bool("Friends", false);
        targetInvisibleSet = bool("Invisible", false);

        throughWallsSet = bool("Through Walls", false);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        // NO WEAPON
        if (!noWeaponSet.boolValue) {
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

        if (!(target instanceof LivingEntity living)) {
            targetSeenAt = 0;
            return;
        }
        if (target == mc.player) return;
        if (!target.isAlive()) return;

        // === Проверка категории цели ===
        if (!isValidTarget(living)) {
            targetSeenAt = 0;
            return;
        }

        // === Через блоки ===
        if (!throughWallsSet.boolValue) {
            if (!mc.player.canSee(target)) {
                targetSeenAt = 0;
                return;
            }
        }

        // === Дистанция ===
        double dist = mc.player.distanceTo(target);
        if (dist > rangeSet.value) {
            targetSeenAt = 0;
            return;
        }

        // === Crit Only ===
        if (critOnlySet.boolValue) {
            if (!canCrit(mc)) return;
        }

        // === Задержка ===
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

        // === Удар ===
        if (mc.getNetworkHandler() != null) {
            PlayerInteractEntityC2SPacket packet = PlayerInteractEntityC2SPacket.attack(
                    target, mc.player.isSneaking());
            mc.getNetworkHandler().sendPacket(packet);
            mc.player.swingHand(Hand.MAIN_HAND);
            lastAttack = now;
            targetSeenAt = now;
        }
    }

    private boolean isValidTarget(LivingEntity target) {
        // Невидимые — отдельная настройка
        if (target.isInvisible()) {
            return targetInvisibleSet.boolValue;
        }

        // Игроки
        if (target instanceof PlayerEntity player) {
            // Проверка на друзей — пока заглушка (системы друзей нет)
            boolean isFriend = false; // TODO: добавить систему друзей
            if (isFriend) {
                return targetFriendsSet.boolValue;
            }

            // Игроки без брони
            if (!hasArmor(player)) {
                return targetNoArmorSet.boolValue || targetPlayersSet.boolValue;
            }

            return targetPlayersSet.boolValue;
        }

        // Враждебные мобы
        if (target instanceof HostileEntity) {
            return targetMobsSet.boolValue;
        }

        // Животные
        if (target instanceof PassiveEntity) {
            return targetAnimalsSet.boolValue;
        }

        return false;
    }

    private boolean hasArmor(PlayerEntity player) {
        return !player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD).isEmpty()
            || !player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST).isEmpty()
            || !player.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS).isEmpty()
            || !player.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET).isEmpty();
    }

    private boolean canCrit(MinecraftClient mc) {
        return !mc.player.isOnGround()
            && !mc.player.isTouchingWater()
            && !mc.player.isClimbing()
            && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
            && mc.player.getVehicle() == null;
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
