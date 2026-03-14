package com.circletimer.client.zone;

import com.circletimer.client.config.CircleConfigStore;
import com.circletimer.client.state.ConfigData;
import com.circletimer.client.state.HudSettingsData;
import com.circletimer.client.state.WorldProfileData;
import com.circletimer.client.state.ZoneData;
import com.circletimer.client.state.ZoneStatsData;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class ZoneManager {
    private static final long AUTO_SAVE_INTERVAL_MS = 5_000L;

    private final CircleConfigStore configStore;
    private ConfigData configData;
    private String currentWorldKey;
    private WorldProfileData currentProfile;
    private boolean dirty;
    private long lastSaveAt;

    private Vec3d pendingPos1;
    private Vec3d pendingPos2;

    public ZoneManager(CircleConfigStore configStore) {
        this.configStore = configStore;
        this.configData = configStore.load();
        this.lastSaveAt = System.currentTimeMillis();
    }

    public void switchWorld(String worldKey) {
        if (Objects.equals(currentWorldKey, worldKey)) {
            return;
        }
        saveNow();
        currentWorldKey = worldKey;
        currentProfile = configData.profiles.computeIfAbsent(worldKey, key -> new WorldProfileData());
        ensureDefaults(currentProfile);
        pendingPos1 = null;
        pendingPos2 = null;
        markDirty();
    }

    public void clearWorld() {
        saveNow();
        currentWorldKey = null;
        currentProfile = null;
        pendingPos1 = null;
        pendingPos2 = null;
    }

    public boolean hasActiveWorld() {
        return currentProfile != null;
    }

    public String getCurrentWorldKey() {
        return currentWorldKey;
    }

    public ZoneData createZone(double x1, double y1, double z1, double x2, double y2, double z2) {
        WorldProfileData profile = requireProfile();
        int id = profile.nextZoneId++;
        ZoneData zone = new ZoneData(id, x1, y1, z1, x2, y2, z2);
        profile.zones.add(zone);
        profile.zones.sort(Comparator.comparingInt(z -> z.id));
        profile.statsByZoneId.computeIfAbsent(id, ignored -> new ZoneStatsData());
        if (profile.activeZoneId == null) {
            profile.activeZoneId = id;
        }
        if (profile.startZoneId == null) {
            profile.startZoneId = id;
        }
        markDirty();
        saveNow();
        return zone;
    }

    public void setPendingPos1(Vec3d pos) {
        pendingPos1 = pos;
    }

    public void setPendingPos2(Vec3d pos) {
        pendingPos2 = pos;
    }

    public Vec3d getPendingPos1() {
        return pendingPos1;
    }

    public Vec3d getPendingPos2() {
        return pendingPos2;
    }

    public boolean hasPendingPair() {
        return pendingPos1 != null && pendingPos2 != null;
    }

    public String formatVec(Vec3d vec) {
        if (vec == null) {
            return "-";
        }
        return String.format(Locale.ROOT, "%.2f %.2f %.2f", vec.x, vec.y, vec.z);
    }

    public List<ZoneData> getZones() {
        if (currentProfile == null) {
            return List.of();
        }
        return new ArrayList<>(currentProfile.zones);
    }

    public Optional<ZoneData> getZoneById(int id) {
        if (currentProfile == null) {
            return Optional.empty();
        }
        return currentProfile.zones.stream().filter(z -> z.id == id).findFirst();
    }

    public Optional<ZoneData> getActiveZone() {
        if (currentProfile == null || currentProfile.activeZoneId == null) {
            return Optional.empty();
        }
        return getZoneById(currentProfile.activeZoneId);
    }

    public boolean selectZone(int id) {
        if (currentProfile == null) {
            return false;
        }
        if (getZoneById(id).isEmpty()) {
            return false;
        }
        currentProfile.activeZoneId = id;
        markDirty();
        saveNow();
        return true;
    }

    public boolean removeZone(int id) {
        if (currentProfile == null) {
            return false;
        }
        boolean removed = currentProfile.zones.removeIf(z -> z.id == id);
        if (!removed) {
            return false;
        }
        currentProfile.statsByZoneId.remove(id);
        if (Objects.equals(currentProfile.activeZoneId, id)) {
            currentProfile.activeZoneId = null;
        }
        if (Objects.equals(currentProfile.startZoneId, id)) {
            currentProfile.startZoneId = null;
            if (!currentProfile.zones.isEmpty()) {
                currentProfile.startZoneId = currentProfile.zones.get(0).id;
            }
        }
        markDirty();
        saveNow();
        return true;
    }

    public boolean resetStats(Integer id) {
        if (currentProfile == null) {
            return false;
        }
        int targetId;
        if (id != null) {
            targetId = id;
        } else {
            if (currentProfile.activeZoneId == null) {
                return false;
            }
            targetId = currentProfile.activeZoneId;
        }
        if (getZoneById(targetId).isEmpty()) {
            return false;
        }
        currentProfile.statsByZoneId.put(targetId, new ZoneStatsData());
        markDirty();
        saveNow();
        return true;
    }

    public ZoneStatsData getStatsForZone(int id) {
        WorldProfileData profile = requireProfile();
        return profile.statsByZoneId.computeIfAbsent(id, ignored -> new ZoneStatsData());
    }

    public HudSettingsData getHudSettings() {
        if (currentProfile == null) {
            return HudSettingsData.defaults();
        }
        ensureDefaults(currentProfile);
        return currentProfile.hud;
    }

    public Integer getActiveZoneId() {
        return currentProfile == null ? null : currentProfile.activeZoneId;
    }

    public int getTargetZoneCount() {
        return getTargetLapCount();
    }

    public void setTargetZoneCount(int count) {
        setTargetLapCount(count);
    }

    public int getCurrentZoneCount() {
        return currentProfile == null ? 0 : currentProfile.zones.size();
    }

    public int getTargetLapCount() {
        return currentProfile == null ? 25 : Math.max(1, currentProfile.targetLapCount);
    }

    public void setTargetLapCount(int count) {
        WorldProfileData profile = requireProfile();
        profile.targetLapCount = Math.max(1, count);
        markDirty();
        saveNow();
    }

    public Integer getStartZoneId() {
        return currentProfile == null ? null : currentProfile.startZoneId;
    }

    public boolean setStartZoneId(int id) {
        if (currentProfile == null) {
            return false;
        }
        if (getZoneById(id).isEmpty()) {
            return false;
        }
        currentProfile.startZoneId = id;
        markDirty();
        saveNow();
        return true;
    }

    public long getBestLapMillis() {
        return currentProfile == null ? 0L : currentProfile.bestLapMillis;
    }

    public void setBestLapMillis(long lapMillis) {
        if (currentProfile == null) {
            return;
        }
        long clamped = Math.max(0L, lapMillis);
        if (currentProfile.bestLapMillis != clamped) {
            currentProfile.bestLapMillis = clamped;
            markDirty();
            saveNow();
        }
    }

    public void updateBestLap(long lapMillis) {
        if (currentProfile == null || lapMillis <= 0L) {
            return;
        }
        if (currentProfile.bestLapMillis <= 0L || lapMillis < currentProfile.bestLapMillis) {
            currentProfile.bestLapMillis = lapMillis;
            markDirty();
            saveNow();
        }
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void maybeAutoSave(long nowMillis) {
        if (!dirty) {
            return;
        }
        if (nowMillis - lastSaveAt >= AUTO_SAVE_INTERVAL_MS) {
            saveNow();
        }
    }

    public void saveNow() {
        if (!dirty) {
            return;
        }
        configStore.save(configData);
        dirty = false;
        lastSaveAt = System.currentTimeMillis();
    }

    private WorldProfileData requireProfile() {
        if (currentProfile == null) {
            throw new IllegalStateException("World profile is not selected");
        }
        ensureDefaults(currentProfile);
        return currentProfile;
    }

    private static void ensureDefaults(WorldProfileData profile) {
        if (profile.zones == null) {
            profile.zones = new ArrayList<>();
        }
        if (profile.statsByZoneId == null) {
            profile.statsByZoneId = new java.util.HashMap<>();
        }
        if (profile.hud == null) {
            profile.hud = HudSettingsData.defaults();
        } else {
            sanitizeHud(profile.hud);
        }
        if (profile.startZoneId == null && profile.zones != null && !profile.zones.isEmpty()) {
            profile.startZoneId = profile.zones.get(0).id;
        }
        if (profile.targetLapCount <= 0) {
            if (profile.targetZoneCount > 0) {
                profile.targetLapCount = profile.targetZoneCount;
            } else {
                profile.targetLapCount = 25;
            }
        }
    }

    private static void sanitizeHud(HudSettingsData hud) {
        HudSettingsData defaults = HudSettingsData.defaults();
        if (hud.scale < 0.5d || hud.scale > 6.0d) {
            hud.scale = defaults.scale;
        }
        if (hud.offsetX < -5000 || hud.offsetX > 5000) {
            hud.offsetX = defaults.offsetX;
        }
        if (hud.offsetY < 0 || hud.offsetY > 5000) {
            hud.offsetY = defaults.offsetY;
        }
        if (hud.horizontalAlign == null) {
            hud.horizontalAlign = defaults.horizontalAlign;
        } else {
            String align = hud.horizontalAlign.toLowerCase(Locale.ROOT);
            if (!"left".equals(align) && !"center".equals(align) && !"right".equals(align)) {
                hud.horizontalAlign = defaults.horizontalAlign;
            } else {
                hud.horizontalAlign = align;
            }
        }
        if (hud.backgroundAlpha < 0 || hud.backgroundAlpha > 255) {
            hud.backgroundAlpha = defaults.backgroundAlpha;
        }
        if ((hud.colorLabel & 0x00FFFFFF) == 0) {
            hud.colorLabel = defaults.colorLabel;
        }
        if ((hud.colorTotal & 0x00FFFFFF) == 0) {
            hud.colorTotal = defaults.colorTotal;
        }
        if ((hud.colorBest & 0x00FFFFFF) == 0) {
            hud.colorBest = defaults.colorBest;
        }
        if ((hud.colorCurrent & 0x00FFFFFF) == 0) {
            hud.colorCurrent = defaults.colorCurrent;
        }
    }
}