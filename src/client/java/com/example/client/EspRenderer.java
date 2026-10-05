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
        if (!isEspEnabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        if (mc.options.hudHidden) return;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        float yaw = (float) Math.toRadians(mc.gameRenderer.getCamera().getYaw());
        float pitch = (float) -Math.toRadians(mc.gameRenderer.getCamera().getPitch());
        double fov = mc.options.getFov().getValue();

        float cosY = (float) Math.cos(yaw);
        float sinY = (float) Math.sin(yaw);
        float cosP = (float) Math.cos(pitch);
        float sinP = (float) Math.sin(pitch);

        double fovRad = Math.toRadians(fov);

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof PlayerEntity p)) continue;
            if (p == mc.player) continue;
            if (!p.isAlive()) continue;

            Vec3d pos = p.getPos();
            double h = p.getHeight();
            Vec3d center = new Vec3d(pos.x, pos.y + h / 2, pos.z);
            Vec3d rel = center.subtract(cam);

            // Yaw rotation
            double rx = -(rel.x * cosY + rel.z * sinY);
            double rz = -rel.x * sinY + rel.z * cosY;

            // Pitch rotation
            double ry = rel.y * cosP - rz * sinP;
            double rz2 = rel.y * sinP + rz * cosP;

            if (rz2 <= 0.1) continue;

            double scale = (sh / 2.0) / Math.tan(fovRad / 2.0);
            int sx = (int)(sw / 2.0 + rx * scale / rz2);
            int sy = (int)(sh / 2.0 - ry * scale / rz2);

            // размер бокса в пикселях (без минимума, чтобы не было "жирным" вдали)
            int boxWidth = (int)(p.getWidth() * scale / rz2);
            int boxHeight = (int)(h * scale / rz2);

            if (boxWidth < 1) boxWidth = 1;
            if (boxHeight < 1) boxHeight = 1;
            if (boxWidth > 200) boxWidth = 200;
            if (boxHeight > 400) boxHeight = 400;

            int x1 = sx - boxWidth / 2;
            int y1 = sy - boxHeight / 2;
            int x2 = sx + boxWidth / 2;
            int y2 = sy + boxHeight / 2;

            // толщина линии зависит от размера бокса
            int t;
            if (boxWidth > 40) t = 2;
            else if (boxWidth > 15) t = 1;
            else t = 1;

            drawOutline(ctx, x1, y1, x2, y2, 0xFFFF3333, t);
        }
    }

    private static void drawOutline(DrawContext ctx, int x1, int y1, int x2, int y2, int color, int t) {
        if (t < 1) t = 1;
        // верх
        ctx.fill(x1, y1, x2, y1 + t, color);
        // низ
        ctx.fill(x1, y2 - t, x2, y2, color);
        // лево
        ctx.fill(x1, y1, x1 + t, y2, color);
        // право
        ctx.fill(x2 - t, y1, x2, y2, color);
    }

    private static boolean isEspEnabled() {
        for (var m : ModuleManager.modules) {
            if (m.name.equals("ESP") && m.enabled) return true;
        }
        return false;
    }
                }
