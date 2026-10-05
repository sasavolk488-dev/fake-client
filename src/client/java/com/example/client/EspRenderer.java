package com.example.client;

import com.example.client.module.ModuleManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class EspRenderer {

    public static void register() {
        HudRenderCallback.EVENT.register(EspRenderer::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        if (mc.options.hudHidden) return;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        // ТЕСТ: красный квадрат по центру экрана
        ctx.fill(sw / 2 - 50, sh / 2 - 50, sw / 2 + 50, sh / 2 + 50, 0xFFFF0000);

        if (!isEspEnabled()) return;

        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        float yaw = (float) Math.toRadians(mc.gameRenderer.getCamera().getYaw());
        float pitch = (float) Math.toRadians(mc.gameRenderer.getCamera().getPitch());
        double fov = mc.options.getFov().getValue();

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof PlayerEntity p)) continue;
            if (p == mc.player) continue;
            if (!p.isAlive()) continue;

            Vec3d pos = p.getPos();
            Vec3d center = new Vec3d(pos.x, pos.y + p.getHeight() / 2, pos.z);
            Vec3d rel = center.subtract(cam);

            double rx = rel.x * Math.cos(-yaw) - rel.z * Math.sin(-yaw);
            double rz = rel.x * Math.sin(-yaw) + rel.z * Math.cos(-yaw);

            double ry = rel.y * Math.cos(-pitch) - rz * Math.sin(-pitch);
            double rz2 = rel.y * Math.sin(-pitch) + rz * Math.cos(-pitch);

            if (rz2 <= 0.1) continue;

            double scale = sh / (2.0 * Math.tan(Math.toRadians(fov) / 2.0));
            int sx = (int)(sw / 2.0 + rx * scale / rz2);
            int sy = (int)(sh / 2.0 - ry * scale / rz2);

            int boxHeight = (int)(p.getHeight() * scale / rz2);
            int boxWidth = (int)(0.6 * scale / rz2);
            if (boxWidth < 8) boxWidth = 8;
            if (boxHeight < 12) boxHeight = 12;
            if (boxWidth > 250) boxWidth = 250;
            if (boxHeight > 400) boxHeight = 400;

            int x1 = sx - boxWidth / 2;
            int y1 = sy - boxHeight / 2;
            int x2 = sx + boxWidth / 2;
            int y2 = sy + boxHeight / 2;

            drawOutline(ctx, x1, y1, x2, y2, 0xFFFF3333);
        }
    }

    private static void drawOutline(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        int t = 2;
        ctx.fill(x1, y1, x2, y1 + t, color);
        ctx.fill(x1, y2 - t, x2, y2, color);
        ctx.fill(x1, y1, x1 + t, y2, color);
        ctx.fill(x2 - t, y1, x2, y2, color);
    }

    private static boolean isEspEnabled() {
        for (var m : ModuleManager.modules) {
            if (m.name.equals("ESP") && m.enabled) return true;
        }
        return false;
    }
          }
