package com.circletimer.client.integration;

import com.circletimer.client.CircleTimerClient;
import com.circletimer.client.hud.HudSettingsScreenFactory;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;

public class CircleModMenuApi implements ModMenuApi {
    @Override
    public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
        return parent -> {
            CircleTimerClient client = CircleTimerClient.getInstance();
            if (client == null || client.getZoneManager() == null || client.getTimerService() == null) {
                return parent;
            }
            return HudSettingsScreenFactory.create(parent, client.getZoneManager(), client.getTimerService());
        };
    }
}