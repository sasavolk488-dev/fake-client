package com.example.client;

import com.example.client.module.ModuleManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class EspRenderer {

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!isEspEnabled()) return;

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world == null || mc.player == null) return;

            MatrixStack matrices = context.matrixStack();
            if (matrices == null) return;

            Vec3d camPos = context.camera().getPos();
            VertexConsumerProvider.Immediate consumers = mc.getBufferBuilders().getEntityVertexConsumers();
            VertexConsumer buffer = consumers.getBuffer(RenderLayer.getLines());

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.lineWidth(4.0f);

            for (Entity entity : mc.world.getEntities()) {
                if (entity == mc.player) continue;
                if (!entity.isAlive()) continue;

                // временно: рисуем ВСЕМ сущностям, чтобы проверить работу рендера
                Box box = entity.getBoundingBox().offset(-camPos.x, -camPos.y, -camPos.z);
                drawBox(matrices, buffer, box);
            }

            consumers.draw();

            RenderSystem.lineWidth(1.0f);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        });
    }

    private static boolean isEspEnabled() {
        for (var m : ModuleManager.modules) {
            if (m.name.equals("ESP") && m.enabled) return true;
        }
        return false;
    }

    private static void drawBox(MatrixStack matrices, VertexConsumer buffer, Box box) {
        Matrix4f m = matrices.peek().getPositionMatrix();
        int color = 0xFFFF0000;

        float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
        float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;

        line(m, buffer, x1, y1, z1, x2, y1, z1, color);
        line(m, buffer, x2, y1, z1, x2, y1, z2, color);
        line(m, buffer, x2, y1, z2, x1, y1, z2, color);
        line(m, buffer, x1, y1, z2, x1, y1, z1, color);

        line(m, buffer, x1, y2, z1, x2, y2, z1, color);
        line(m, buffer, x2, y2, z1, x2, y2, z2, color);
        line(m, buffer, x2, y2, z2, x1, y2, z2, color);
        line(m, buffer, x1, y2, z2, x1, y2, z1, color);

        line(m, buffer, x1, y1, z1, x1, y2, z1, color);
        line(m, buffer, x2, y1, z1, x2, y2, z1, color);
        line(m, buffer, x2, y1, z2, x2, y2, z2, color);
        line(m, buffer, x1, y1, z2, x1, y2, z2, color);
    }

    private static void line(Matrix4f m, VertexConsumer buf,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             int color) {
        buf.vertex(m, x1, y1, z1).color(color);
        buf.vertex(m, x2, y2, z2).color(color);
    }
                }
