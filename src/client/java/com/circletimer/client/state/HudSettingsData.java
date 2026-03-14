package com.circletimer.client.state;

public class HudSettingsData {
    public boolean visible = true;
    public int offsetX = 12;
    public int offsetY = 40;
    public double scale = 1.0d;
    public int colorLabel = 0xDDDDDD;
    public int colorTotal = 0xFFFFFF;
    public int colorBest = 0x55FF55;
    public int colorCurrent = 0x55FFFF;
    public String horizontalAlign = "right"; // right|left|center
    public int backgroundAlpha = 127; // 0..255

    public static HudSettingsData defaults() {
        return new HudSettingsData();
    }
}