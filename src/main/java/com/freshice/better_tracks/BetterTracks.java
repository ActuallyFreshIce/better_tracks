package com.freshice.better_tracks;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BetterTracks implements ModInitializer {
    public static final String MOD_ID = "better_tracks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private final Map<UUID, PathData> paths = new HashMap<>();

    @Override
    public void onInitialize() {
        UseBlockCallback.EVENT.register(this::onUseBlock);
        LOGGER.info("BetterTracks initialized");
    }

    private ActionResult onUseBlock(net.minecraft.entity.player.PlayerEntity player, net.minecraft.world.World world, net.minecraft.util.Hand hand, BlockHitResult hitResult) {
        if (world.isClient) return ActionResult.PASS;
        if (!(player instanceof ServerPlayerEntity)) return ActionResult.PASS;
        BlockPos pos = hitResult.getBlockPos();

        PathData data = paths.computeIfAbsent(player.getUuid(), u -> new PathData());
        long time = world.getTime();
        if (time - data.lastClickTime < 10) {
            // Double click detected, finalize path
            if (!data.points.isEmpty()) {
                drawPath((ServerWorld) world, data.points);
                data.points.clear();
            }
            return ActionResult.SUCCESS;
        }

        data.lastClickTime = time;
        data.points.add(pos.toCenterPos());
        int size = data.points.size();
        if (size > 1) {
            drawLine((ServerWorld) world, data.points.get(size - 2), data.points.get(size - 1));
        }

        return ActionResult.SUCCESS;
    }

    private void drawPath(ServerWorld world, List<Vec3d> points) {
        for (int i = 1; i < points.size(); i++) {
            drawLine(world, points.get(i - 1), points.get(i));
        }
    }

    private void drawLine(ServerWorld world, Vec3d start, Vec3d end) {
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double dz = end.z - start.z;
        int steps = (int) (Math.sqrt(dx * dx + dy * dy + dz * dz) * 4); // 1/4 block spacing
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double x = start.x + dx * t;
            double y = start.y + dy * t;
            double z = start.z + dz * t;
            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    private static class PathData {
        long lastClickTime = -20;
        final List<Vec3d> points = new ArrayList<>();
    }
}
