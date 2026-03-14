package com.circletimer.client.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldProfileData {
    public int nextZoneId = 1;
    // Legacy field from older builds; kept for config compatibility.
    public int targetZoneCount = 0;
    public int targetLapCount = 25;
    public Integer activeZoneId = null;
    public Integer startZoneId = null;
    public long bestLapMillis = 0L;
    public List<ZoneData> zones = new ArrayList<>();
    public Map<Integer, ZoneStatsData> statsByZoneId = new HashMap<>();
    public HudSettingsData hud = HudSettingsData.defaults();
}
