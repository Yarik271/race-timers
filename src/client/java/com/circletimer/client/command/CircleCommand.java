package com.circletimer.client.command;

import com.circletimer.client.state.ZoneData;
import com.circletimer.client.timer.FlightTimerService;
import com.circletimer.client.zone.ZoneManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class CircleCommand {
    private CircleCommand() {
    }

    public static void register(ZoneManager zoneManager, FlightTimerService timerService) {
        ClientCommandRegistrationCallback.EVENT.register(
            (dispatcher, registryAccess) -> registerCommands(dispatcher, registryAccess, zoneManager, timerService)
        );
    }

    private static void registerCommands(
        CommandDispatcher<FabricClientCommandSource> dispatcher,
        CommandRegistryAccess registryAccess,
        ZoneManager zoneManager,
        FlightTimerService timerService
    ) {
        dispatcher.register(
            ClientCommandManager.literal("circle")
                .then(ClientCommandManager.argument("x1", StringArgumentType.word())
                    .then(ClientCommandManager.argument("y1", StringArgumentType.word())
                        .then(ClientCommandManager.argument("z1", StringArgumentType.word())
                            .then(ClientCommandManager.argument("x2", StringArgumentType.word())
                                .then(ClientCommandManager.argument("y2", StringArgumentType.word())
                                    .then(ClientCommandManager.argument("z2", StringArgumentType.word())
                                        .executes(ctx -> createZoneFromArgs(ctx, zoneManager, timerService))
                                    )
                                )
                            )
                        )
                    )
                )
                .then(ClientCommandManager.literal("pos1")
                    .executes(ctx -> setPosFromPlayer(ctx, zoneManager, true))
                    .then(ClientCommandManager.argument("x", StringArgumentType.word())
                        .then(ClientCommandManager.argument("y", StringArgumentType.word())
                            .then(ClientCommandManager.argument("z", StringArgumentType.word())
                                .executes(ctx -> setPosFromXYZ(ctx, zoneManager, true))
                            )
                        )
                    )
                )
                .then(ClientCommandManager.literal("pos2")
                    .executes(ctx -> setPosFromPlayer(ctx, zoneManager, false))
                    .then(ClientCommandManager.argument("x", StringArgumentType.word())
                        .then(ClientCommandManager.argument("y", StringArgumentType.word())
                            .then(ClientCommandManager.argument("z", StringArgumentType.word())
                                .executes(ctx -> setPosFromXYZ(ctx, zoneManager, false))
                            )
                        )
                    )
                )
                .then(ClientCommandManager.literal("create").executes(ctx -> createFromSelected(ctx, zoneManager, timerService)))
                .then(ClientCommandManager.literal("list").executes(ctx -> listZones(ctx, zoneManager)))
                .then(ClientCommandManager.literal("target")
                    .executes(ctx -> showTarget(ctx, zoneManager))
                    .then(ClientCommandManager.argument("count", IntegerArgumentType.integer(1))
                        .executes(ctx -> setTarget(ctx, zoneManager))
                    )
                )
                .then(ClientCommandManager.literal("start")
                    .executes(ctx -> showStart(ctx, zoneManager, timerService))
                    .then(ClientCommandManager.argument("id", IntegerArgumentType.integer(1))
                        .executes(ctx -> setStart(ctx, zoneManager, timerService))
                    )
                )
                .then(ClientCommandManager.literal("remove")
                    .then(ClientCommandManager.argument("id", IntegerArgumentType.integer(1))
                        .executes(ctx -> removeZone(ctx, zoneManager, timerService))
                    )
                )
                .then(ClientCommandManager.literal("run")
                    .then(ClientCommandManager.literal("reset").executes(ctx -> resetRun(ctx, timerService)))
                    .then(ClientCommandManager.literal("stop").executes(ctx -> stopRun(ctx, timerService)))
                    .then(ClientCommandManager.literal("status").executes(ctx -> runStatus(ctx, timerService)))
                )
        );
    }

    private static int createZoneFromArgs(
        CommandContext<FabricClientCommandSource> ctx,
        ZoneManager zoneManager,
        FlightTimerService timerService
    ) {
        Vec3d base = ctx.getSource().getPosition();
        try {
            Vec3d pos1 = parseVec(ctx, base, "x1", "y1", "z1");
            Vec3d pos2 = parseVec(ctx, base, "x2", "y2", "z2");
            return createZone(ctx, zoneManager, timerService, pos1, pos2);
        } catch (IllegalArgumentException ex) {
            ctx.getSource().sendError(Text.literal(ex.getMessage()));
            return 0;
        }
    }

    private static int setPosFromPlayer(CommandContext<FabricClientCommandSource> ctx, ZoneManager zoneManager, boolean first) {
        Vec3d pos = ctx.getSource().getPosition();
        if (first) {
            zoneManager.setPendingPos1(pos);
            ctx.getSource().sendFeedback(Text.translatable("message.circletimer.pos1_set", zoneManager.formatVec(pos)));
        } else {
            zoneManager.setPendingPos2(pos);
            ctx.getSource().sendFeedback(Text.translatable("message.circletimer.pos2_set", zoneManager.formatVec(pos)));
        }
        return 1;
    }

    private static int setPosFromXYZ(CommandContext<FabricClientCommandSource> ctx, ZoneManager zoneManager, boolean first) {
        Vec3d base = ctx.getSource().getPosition();
        try {
            Vec3d pos = parseVec(ctx, base, "x", "y", "z");
            if (first) {
                zoneManager.setPendingPos1(pos);
                ctx.getSource().sendFeedback(Text.translatable("message.circletimer.pos1_set", zoneManager.formatVec(pos)));
            } else {
                zoneManager.setPendingPos2(pos);
                ctx.getSource().sendFeedback(Text.translatable("message.circletimer.pos2_set", zoneManager.formatVec(pos)));
            }
            return 1;
        } catch (IllegalArgumentException ex) {
            ctx.getSource().sendError(Text.literal(ex.getMessage()));
            return 0;
        }
    }

    private static int createFromSelected(
        CommandContext<FabricClientCommandSource> ctx,
        ZoneManager zoneManager,
        FlightTimerService timerService
    ) {
        Vec3d pos1 = zoneManager.getPendingPos1();
        Vec3d pos2 = zoneManager.getPendingPos2();
        if (pos1 == null || pos2 == null) {
            ctx.getSource().sendError(Text.translatable("error.circletimer.pos_pair_missing"));
            return 0;
        }
        return createZone(ctx, zoneManager, timerService, pos1, pos2);
    }

    private static int createZone(
        CommandContext<FabricClientCommandSource> ctx,
        ZoneManager zoneManager,
        FlightTimerService timerService,
        Vec3d pos1,
        Vec3d pos2
    ) {
        if (!zoneManager.hasActiveWorld()) {
            ctx.getSource().sendError(Text.translatable("error.circletimer.world_not_ready"));
            return 0;
        }

        ZoneData zone = zoneManager.createZone(pos1.x, pos1.y, pos1.z, pos2.x, pos2.y, pos2.z);
        timerService.highlightZone(zone.id);
        ctx.getSource().sendFeedback(Text.translatable(
            "message.circletimer.zone_created",
            zone.id,
            format(zone.minX), format(zone.minY), format(zone.minZ),
            format(zone.maxX), format(zone.maxY), format(zone.maxZ)
        ));
        return 1;
    }

    private static int listZones(CommandContext<FabricClientCommandSource> ctx, ZoneManager zoneManager) {
        if (!zoneManager.hasActiveWorld()) {
            ctx.getSource().sendFeedback(Text.translatable("message.circletimer.profile_not_ready"));
            return 1;
        }
        List<ZoneData> zones = zoneManager.getZones().stream()
            .sorted(Comparator.comparingInt(z -> z.id))
            .toList();
        if (zones.isEmpty()) {
            ctx.getSource().sendFeedback(Text.translatable("message.circletimer.no_zones"));
            return 1;
        }

        Integer startId = zoneManager.getStartZoneId();
        ctx.getSource().sendFeedback(Text.translatable("message.circletimer.zone_count", zones.size()));
        for (ZoneData zone : zones) {
            String startMark = (startId != null && startId == zone.id) ? " *" : "";
            ctx.getSource().sendFeedback(Text.translatable(
                "message.circletimer.zone_line",
                zone.id,
                startMark,
                format(zone.minX), format(zone.minY), format(zone.minZ),
                format(zone.maxX), format(zone.maxY), format(zone.maxZ)
            ));
        }
        return 1;
    }

    private static int removeZone(
        CommandContext<FabricClientCommandSource> ctx,
        ZoneManager zoneManager,
        FlightTimerService timerService
    ) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        if (!zoneManager.removeZone(id)) {
            ctx.getSource().sendError(Text.translatable("error.circletimer.zone_not_found", id));
            return 0;
        }
        timerService.resetRun();
        ctx.getSource().sendFeedback(Text.translatable("message.circletimer.zone_removed", id));
        return 1;
    }

    private static int showTarget(CommandContext<FabricClientCommandSource> ctx, ZoneManager zoneManager) {
        if (!zoneManager.hasActiveWorld()) {
            ctx.getSource().sendError(Text.translatable("error.circletimer.world_not_ready"));
            return 0;
        }
        int target = zoneManager.getTargetLapCount();
        ctx.getSource().sendFeedback(Text.translatable("message.circletimer.target_show", target));
        return 1;
    }

    private static int setTarget(CommandContext<FabricClientCommandSource> ctx, ZoneManager zoneManager) {
        if (!zoneManager.hasActiveWorld()) {
            ctx.getSource().sendError(Text.translatable("error.circletimer.world_not_ready"));
            return 0;
        }
        int count = IntegerArgumentType.getInteger(ctx, "count");
        zoneManager.setTargetLapCount(count);
        ctx.getSource().sendFeedback(Text.translatable("message.circletimer.target_set", count));
        return 1;
    }

    private static int showStart(
        CommandContext<FabricClientCommandSource> ctx,
        ZoneManager zoneManager,
        FlightTimerService timerService
    ) {
        Integer startId = zoneManager.getStartZoneId();
        if (startId == null) {
            ctx.getSource().sendFeedback(Text.translatable("message.circletimer.start_missing"));
            return 1;
        }
        timerService.highlightZone(startId);
        ctx.getSource().sendFeedback(Text.translatable("message.circletimer.start_show", startId));
        return 1;
    }

    private static int setStart(
        CommandContext<FabricClientCommandSource> ctx,
        ZoneManager zoneManager,
        FlightTimerService timerService
    ) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        if (!zoneManager.setStartZoneId(id)) {
            ctx.getSource().sendError(Text.translatable("error.circletimer.zone_not_found", id));
            return 0;
        }
        timerService.resetRun();
        timerService.highlightZone(id);
        ctx.getSource().sendFeedback(Text.translatable("message.circletimer.start_set", id));
        return 1;
    }

    private static int resetRun(CommandContext<FabricClientCommandSource> ctx, FlightTimerService timerService) {
        timerService.resetRun();
        ctx.getSource().sendFeedback(Text.translatable("message.circletimer.run_reset"));
        return 1;
    }

    private static int stopRun(CommandContext<FabricClientCommandSource> ctx, FlightTimerService timerService) {
        timerService.stopRun();
        ctx.getSource().sendFeedback(Text.translatable("message.circletimer.run_stopped"));
        return 1;
    }

    private static int runStatus(CommandContext<FabricClientCommandSource> ctx, FlightTimerService timerService) {
        ctx.getSource().sendFeedback(Text.literal(timerService.getStateDebug()));
        return 1;
    }

    private static Vec3d parseVec(CommandContext<FabricClientCommandSource> ctx, Vec3d base, String ax, String ay, String az) {
        String sx = StringArgumentType.getString(ctx, ax);
        String sy = StringArgumentType.getString(ctx, ay);
        String sz = StringArgumentType.getString(ctx, az);
        return new Vec3d(parseCoord(sx, base.x), parseCoord(sy, base.y), parseCoord(sz, base.z));
    }

    private static double parseCoord(String token, double base) {
        try {
            if (token.startsWith("~")) {
                if (token.length() == 1) {
                    return base;
                }
                return base + Double.parseDouble(token.substring(1));
            }
            if (token.startsWith("^")) {
                throw new IllegalArgumentException(Text.translatable("error.circletimer.local_not_supported").getString());
            }
            return Double.parseDouble(token);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(Text.translatable("error.circletimer.bad_coord", token).getString());
        }
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
