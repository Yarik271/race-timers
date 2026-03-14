package com.circletimer.client.timer;

import com.circletimer.client.state.ZoneData;
import com.circletimer.client.util.TimeFormat;
import com.circletimer.client.zone.ZoneManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Util;

import java.util.List;

public class FlightTimerService {
    private static final long MIN_CROSS_INTERVAL_MS = 100L;
    private static final long DEFAULT_HIGHLIGHT_DURATION_MS = 10_000L;
    private static final int HIGHLIGHT_TICK_INTERVAL = 3;

    private final ZoneManager zoneManager;

    private boolean runActive = false;
    private long runStartMillis = 0L;
    private long lapStartMillis = 0L;
    private long totalMillis = 0L;
    private long currentLapMillis = 0L;
    private int currentCheckpointFromZero = 0;
    private int lapsCompleted = 0;
    private long lastCrossMillis = 0L;

    private boolean hasPosition = false;
    private double lastX;
    private double lastY;
    private double lastZ;
    private boolean wasInsideStart = false;
    private int tickCounter = 0;

    private Integer highlightedZoneId = null;
    private long highlightUntilMillis = 0L;

    public FlightTimerService(ZoneManager zoneManager) {
        this.zoneManager = zoneManager;
    }

    public void onClientTick(MinecraftClient client) {
        long now = Util.getMeasuringTimeMs();
        zoneManager.maybeAutoSave(now);

        if (client.player == null || client.world == null) {
            hasPosition = false;
            return;
        }

        List<ZoneData> zones = zoneManager.getZones();
        if (zones.isEmpty()) {
            hasPosition = false;
            wasInsideStart = false;
            return;
        }

        ZoneData startZone = resolveStartZone(zones);
        if (startZone == null) {
            hasPosition = false;
            wasInsideStart = false;
            return;
        }

        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();

        boolean insideNow = startZone.contains(x, y, z);
        if (!hasPosition) {
            hasPosition = true;
            lastX = x;
            lastY = y;
            lastZ = z;
            wasInsideStart = insideNow;
            return;
        }

        boolean entered = insideNow && !wasInsideStart;
        boolean sweptCross = !insideNow && !wasInsideStart && segmentIntersectsZone(lastX, lastY, lastZ, x, y, z, startZone);
        boolean crossed = (entered || sweptCross) && (now - lastCrossMillis >= MIN_CROSS_INTERVAL_MS);

        if (runActive) {
            totalMillis = Math.max(0L, now - runStartMillis);
            currentLapMillis = Math.max(0L, now - lapStartMillis);
        }

        if (crossed) {
            onStartCircleCross(now);
            lastCrossMillis = now;
        }

        tickCounter++;
        if (tickCounter % HIGHLIGHT_TICK_INTERVAL == 0) {
            renderZoneHighlight(client.world, now);
        }

        wasInsideStart = insideNow;
        lastX = x;
        lastY = y;
        lastZ = z;
    }

    public void flushRunningSegment() {
    }

    public void resetRun() {
        runActive = false;
        runStartMillis = 0L;
        lapStartMillis = 0L;
        totalMillis = 0L;
        currentLapMillis = 0L;
        currentCheckpointFromZero = 0;
        lapsCompleted = 0;
        lastCrossMillis = 0L;
        hasPosition = false;
        wasInsideStart = false;
        zoneManager.setBestLapMillis(0L);
    }

    public void stopRun() {
        if (!runActive) {
            return;
        }
        long now = Util.getMeasuringTimeMs();
        totalMillis = Math.max(0L, now - runStartMillis);
        currentLapMillis = Math.max(0L, now - lapStartMillis);
        runActive = false;
    }

    public void highlightZone(int id) {
        highlightZone(id, DEFAULT_HIGHLIGHT_DURATION_MS);
    }

    public void highlightZone(int id, long durationMillis) {
        if (zoneManager.getZoneById(id).isEmpty()) {
            return;
        }
        highlightedZoneId = id;
        highlightUntilMillis = Util.getMeasuringTimeMs() + Math.max(1L, durationMillis);
    }

    public long getTotalMillis() {
        return totalMillis;
    }

    public long getCurrentLapMillis() {
        return currentLapMillis;
    }

    public long getBestLapMillis() {
        return zoneManager.getBestLapMillis();
    }

    public int getCurrentCheckpointFromZero() {
        return currentCheckpointFromZero;
    }

    public int getLapsCompleted() {
        return lapsCompleted;
    }

    public boolean isRunActive() {
        return runActive;
    }

    public String getStateDebug() {
        return "run=" + runActive
            + ", total=" + TimeFormat.format(totalMillis)
            + ", current=" + TimeFormat.format(currentLapMillis)
            + ", lap=" + currentCheckpointFromZero + "/" + zoneManager.getTargetLapCount()
            + ", done=" + lapsCompleted;
    }

    private ZoneData resolveStartZone(List<ZoneData> zones) {
        Integer startId = zoneManager.getStartZoneId();
        if (startId != null) {
            return zoneManager.getZoneById(startId).orElse(null);
        }

        int fallback = zones.stream().mapToInt(z -> z.id).min().orElse(-1);
        if (fallback < 0) {
            return null;
        }
        zoneManager.setStartZoneId(fallback);
        return zoneManager.getZoneById(fallback).orElse(null);
    }

    private void onStartCircleCross(long now) {
        if (!runActive) {
            runActive = true;
            runStartMillis = now;
            lapStartMillis = now;
            totalMillis = 0L;
            currentLapMillis = 0L;
            currentCheckpointFromZero = 0;
            zoneManager.setBestLapMillis(0L);
            return;
        }

        long lapMillis = Math.max(0L, now - lapStartMillis);
        zoneManager.updateBestLap(lapMillis);
        lapsCompleted++;
        currentCheckpointFromZero++;
        lapStartMillis = now;
        currentLapMillis = 0L;

        if (currentCheckpointFromZero >= zoneManager.getTargetLapCount()) {
            runActive = false;
            totalMillis = Math.max(0L, now - runStartMillis);
        }
    }

    private void renderZoneHighlight(ClientWorld world, long nowMillis) {
        if (highlightedZoneId == null || nowMillis >= highlightUntilMillis) {
            highlightedZoneId = null;
            return;
        }

        ZoneData zone = zoneManager.getZoneById(highlightedZoneId).orElse(null);
        if (zone == null) {
            highlightedZoneId = null;
            return;
        }

        double minX = zone.minX;
        double minY = zone.minY;
        double minZ = zone.minZ;
        double maxX = zone.maxX;
        double maxY = zone.maxY;
        double maxZ = zone.maxZ;

        double step = 1.0d;

        for (double x = minX; x <= maxX; x += step) {
            spawn(world, x, minY, minZ);
            spawn(world, x, minY, maxZ);
            spawn(world, x, maxY, minZ);
            spawn(world, x, maxY, maxZ);
        }
        for (double y = minY; y <= maxY; y += step) {
            spawn(world, minX, y, minZ);
            spawn(world, minX, y, maxZ);
            spawn(world, maxX, y, minZ);
            spawn(world, maxX, y, maxZ);
        }
        for (double z = minZ; z <= maxZ; z += step) {
            spawn(world, minX, minY, z);
            spawn(world, minX, maxY, z);
            spawn(world, maxX, minY, z);
            spawn(world, maxX, maxY, z);
        }
    }

    private static void spawn(ClientWorld world, double x, double y, double z) {
        world.addParticleClient(ParticleTypes.END_ROD, x, y, z, 0.0d, 0.0d, 0.0d);
    }

    private static boolean segmentIntersectsZone(
        double x0, double y0, double z0,
        double x1, double y1, double z1,
        ZoneData zone
    ) {
        double minX = zone.minX;
        double minY = zone.minY;
        double minZ = zone.minZ;
        double maxX = zone.maxX;
        double maxY = zone.maxY;
        double maxZ = zone.maxZ;

        double tMin = 0.0d;
        double tMax = 1.0d;

        double[] tx = clipAxis(x0, x1, minX, maxX, tMin, tMax);
        if (tx == null) {
            return false;
        }
        tMin = tx[0];
        tMax = tx[1];

        double[] ty = clipAxis(y0, y1, minY, maxY, tMin, tMax);
        if (ty == null) {
            return false;
        }
        tMin = ty[0];
        tMax = ty[1];

        double[] tz = clipAxis(z0, z1, minZ, maxZ, tMin, tMax);
        if (tz == null) {
            return false;
        }
        return tz[0] <= tz[1];
    }

    private static double[] clipAxis(double p0, double p1, double min, double max, double tMin, double tMax) {
        double d = p1 - p0;
        if (Math.abs(d) < 1.0e-9) {
            if (p0 < min || p0 > max) {
                return null;
            }
            return new double[]{tMin, tMax};
        }

        double inv = 1.0d / d;
        double t1 = (min - p0) * inv;
        double t2 = (max - p0) * inv;
        if (t1 > t2) {
            double tmp = t1;
            t1 = t2;
            t2 = tmp;
        }

        double ntMin = Math.max(tMin, t1);
        double ntMax = Math.min(tMax, t2);
        if (ntMin > ntMax) {
            return null;
        }
        return new double[]{ntMin, ntMax};
    }
}