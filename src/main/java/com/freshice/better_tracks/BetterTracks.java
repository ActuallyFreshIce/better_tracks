package com.freshice.better_tracks;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import com.freshice.better_tracks.item.WaypointWandItem;
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

    public static final Item WAYPOINT_WAND = new WaypointWandItem(new Item.Settings().maxCount(1));

    public static final ItemGroup ITEM_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(WAYPOINT_WAND))
            .displayName(Text.translatable("itemGroup." + MOD_ID + ".main"))
            .entries((context, entries) -> entries.add(WAYPOINT_WAND))
            .build();

    private static final Map<UUID, PathData> PATHS = new HashMap<>();

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "waypoint_wand"), WAYPOINT_WAND);
        Registry.register(Registries.ITEM_GROUP, new Identifier(MOD_ID, "main"), ITEM_GROUP);

        UseBlockCallback.EVENT.register(this::onUseBlock);
        LOGGER.info("BetterTracks initialized");
    }

    private ActionResult onUseBlock(net.minecraft.entity.player.PlayerEntity player, net.minecraft.world.World world, net.minecraft.util.Hand hand, BlockHitResult hitResult) {
        if (!player.getStackInHand(hand).isOf(WAYPOINT_WAND)) return ActionResult.PASS;
        if (world.isClient || !(player instanceof ServerPlayerEntity)) return ActionResult.SUCCESS;
        BlockPos pos = hitResult.getBlockPos();
        return handleWaypointClick((ServerPlayerEntity) player, (ServerWorld) world, pos);
    }

    private static ActionResult handleWaypointClick(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        PathData data = PATHS.computeIfAbsent(player.getUuid(), u -> new PathData());
        long time = world.getTime();
        if (time - data.lastClickTime < 10) {
            if (!data.points.isEmpty()) {
                drawPath(world, data.points);
                data.points.clear();
            }
            return ActionResult.SUCCESS;
        }

        data.lastClickTime = time;
        data.points.add(pos.toCenterPos());
        int size = data.points.size();
        if (size > 1) {
            drawLine(world, data.points.get(size - 2), data.points.get(size - 1));
        }

        return ActionResult.SUCCESS;
    }

    private static void drawPath(ServerWorld world, List<Vec3d> points) {
        for (int i = 1; i < points.size(); i++) {
            drawLine(world, points.get(i - 1), points.get(i));
        }
    }

    private static void drawLine(ServerWorld world, Vec3d start, Vec3d end) {
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