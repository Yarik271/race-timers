package com.circletimer.client.hud;

import com.circletimer.client.timer.FlightTimerService;
import com.circletimer.client.zone.ZoneManager;
import net.minecraft.client.gui.screen.Screen;

public final class HudSettingsScreenFactory {
    private HudSettingsScreenFactory() {
    }

    public static Screen create(Screen parent, ZoneManager zoneManager, FlightTimerService timerService) {
        return new HudDragSettingsScreen(parent, zoneManager, timerService);
    }
}
