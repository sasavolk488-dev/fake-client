package com.example.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.Random;

public class TriggerBotModule extends Module {

    public final Setting minDelaySet;
    public final Setting maxDelaySet;
    public final Setting rangeSet;
    public final Setting randomizeSet;

    public final Setting critOnlySet;
    public final Setting smartCritsSet;
    public final Setting earlyMaceSet;

    public final Setting throughBlocksSet;
    public final Setting throughPlayersSet;
    public final Setting hitWhileEatingSet;
    public final Setting weaponOnlySet;
    public final Setting sprintResetSet;

    public final Setting targetPlayersSet;
    public final Setting targetNoArmorSet;
    public final Setting targetMobsSet;
    public final Setting targetAnimalsSet;
    public final Setting targetFriendsSet;
    public final Setting targetInvisibleSet;

    public final Setting noGuiSet;
    public final Setting focusOneSet;
    public final Setting whitelistSet;

    private long lastAttack = 0;
    private long targetSeenAt = 0;
    private int currentFocusId = -1;
    private final Random random = new Random();

    public TriggerBotModule() {
        super("TriggerBot", "Атака при наведении", "Combat", false);

        rangeSet = num("Range", 3.0f, 1.0f, 6.0f, 0.1f);
        minDelaySet = num("Min Delay", 60f, 10f, 500f, 5f);
        maxDelaySet = num("Max Delay", 120f, 10f, 500f, 5f);
        randomizeSet = num("Randomize", 15f, 0f, 100f, 1f);

        critOnlySet = bool("Crit Only", false);
        smartCritsSet = bool("Smart Crits", false);
        earlyMaceSet = bool("Early Mace", false);

        throughBlocksSet = bool("Through Blocks", false);
        throughPlayersSet = bool("Through Players", false);
        hitWhileEatingSet = bool("Hit While Eating", false);
        weaponOnlySet = bool("Weapon Only", true);
        sprintResetSet = bool("Sprint Reset", false);

        targetPlayersSet = bool("Players", true);
        targetNoArmorSet = bool("No Armor", false);
        targetMobsSet = bool("Mobs", false);
        targetAnimalsSet = bool("Animals", false);
        targetFriendsSet = bool("Friends", false);
        targetInvisibleSet = bool("Invisible", false);

        noGuiSet = bool("No GUI", true);
        focusOneSet = bool("Focus One", false);
        whitelistSet = bool("Whitelist", false);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        // Не бить в GUI
        if (noGuiSet.boolValue && mc.currentScreen != null) return;

        // Бить пока ешь
        if (!hitWhileEatingSet.boolValue && mc.player.isUsingItem()) {
            targetSeenAt = 0;
            return;
        }

        // Weapon Only
        if (weaponOnlySet.boolValue && !isWeapon(mc)) {
            targetSeenAt = 0;
            return;
        }

        // Луч в прицеле
        HitResult hit = mc.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.ENTITY) {
            targetSeenAt = 0;
            currentFocusId = -1;
            return;
        }

        Entity target = ((EntityHitResult) hit).getEntity();
        if (!(target instanceof LivingEntity living)) {
            targetSeenAt = 0;
            return;
        }
        if (target == mc.player) return;
        if (!target.isAlive()) return;

        if (!isValidTarget(living)) {
            targetSeenAt = 0;
            return;
        }

        // Через игроков
        if (!throughPlayersSet.boolValue && hasPlayerBetween(mc, target)) {
            targetSeenAt = 0;
            return;
        }

        // Через блоки
        if (!throughBlocksSet.boolValue && !mc.player.canSee(target)) {
            targetSeenAt = 0;
            return;
        }

        // Дистанция
        if (mc.player.distanceTo(target) > rangeSet.value) {
            targetSeenAt = 0;
            return;
        }

        // Фокус на одном
        if (focusOneSet.boolValue) {
            if (currentFocusId == -1) currentFocusId = target.getId();
            else if (currentFocusId != target.getId()) return;
        }

        // Умные криты — авто-прыжок
        if (smartCritsSet.boolValue && mc.player.isOnGround()) {
            mc.player.jump();
            return;
        }

        // Только криты
        if (critOnlySet.boolValue && !canCrit(mc)) return;

        // Ранний удар булавой
        if (earlyMaceSet.boolValue && isMace(mc)) {
            if (mc.player.fallDistance > 1.5f && !mc.player.isOnGround()) {
                attack(mc, target);
                return;
            }
        }

        // === 1.9 кулдаун (всегда) ===
        float cooldown = mc.player.getAttackCooldownProgress(0f);
        if (cooldown < 0.95f) return;

        long now = System.currentTimeMillis();
        if (targetSeenAt == 0) {
            targetSeenAt = now;
            return;
        }

        int minDelay = (int) minDelaySet.value;
        int maxDelay = (int) maxDelaySet.value;
        int delay = minDelay + random.nextInt(Math.max(1, maxDelay - minDelay + 1));

        int randAmt = (int) randomizeSet.value;
        if (randAmt > 0) delay += random.nextInt(randAmt * 2) - randAmt;
        delay = Math.max(20, delay);

        if (now - targetSeenAt < delay) return;
        if (now - lastAttack < delay) return;

        attack(mc, target);

        if (sprintResetSet.boolValue) {
            mc.player.setSprinting(false);
            mc.player.setSprinting(true);
        }
    }

    private void attack(MinecraftClient mc, Entity target) {
        if (mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendPacket(
                PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking())
        );
        mc.player.swingHand(Hand.MAIN_HAND);
        lastAttack = System.currentTimeMillis();
        targetSeenAt = System.currentTimeMillis();
    }

    private boolean hasPlayerBetween(MinecraftClient mc, Entity target) {
        var box = mc.player.getBoundingBox()
                .stretch(target.getPos().subtract(mc.player.getPos()))
                .expand(1.0);

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || e == target) continue;
            if (!(e instanceof PlayerEntity)) continue;
            if (box.intersects(e.getBoundingBox())) return true;
        }
        return false;
    }

    private boolean isValidTarget(LivingEntity target) {
        if (target.isInvisible()) return targetInvisibleSet.boolValue;

        if (target instanceof PlayerEntity player) {
            boolean isFriend = false;
            if (isFriend) return targetFriendsSet.boolValue;

            if (!hasArmor(player)) return targetNoArmorSet.boolValue || targetPlayersSet.boolValue;
            return targetPlayersSet.boolValue;
        }

        if (target instanceof HostileEntity) return targetMobsSet.boolValue;
        if (target instanceof PassiveEntity) return targetAnimalsSet.boolValue;
        return false;
    }

    private boolean hasArmor(PlayerEntity player) {
        return !player.getEquippedStack(EquipmentSlot.HEAD).isEmpty()
            || !player.getEquippedStack(EquipmentSlot.CHEST).isEmpty()
            || !player.getEquippedStack(EquipmentSlot.LEGS).isEmpty()
            || !player.getEquippedStack(EquipmentSlot.FEET).isEmpty();
    }

    private boolean canCrit(MinecraftClient mc) {
        return !mc.player.isOnGround()
            && !mc.player.isTouchingWater()
            && !mc.player.isClimbing()
            && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
            && mc.player.getVehicle() == null;
    }

    private boolean isWeapon(MinecraftClient mc) {
        Item item = mc.player.getMainHandStack().getItem();
        return item instanceof SwordItem
            || item instanceof AxeItem
            || item instanceof TridentItem;
    }

    private boolean isMace(MinecraftClient mc) {
        String n = mc.player.getMainHandStack().getItem().toString().toLowerCase();
        return n.contains("mace");
    }

    @Override
    public void toggle() {
        super.toggle();
        targetSeenAt = 0;
        lastAttack = 0;
        currentFocusId = -1;
    }
    }
