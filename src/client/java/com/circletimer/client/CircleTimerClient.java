package com.circletimer.client;

import com.circletimer.client.command.CircleCommand;
import com.circletimer.client.config.CircleConfigStore;
import com.circletimer.client.hud.CircleHudRenderer;
import com.circletimer.client.timer.FlightTimerService;
import com.circletimer.client.world.WorldKeyResolver;
import com.circletimer.client.zone.ZoneManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CircleTimerClient implements ClientModInitializer {
    public static final String MOD_ID = "circletimer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static CircleTimerClient INSTANCE;

    private ZoneManager zoneManager;
    private FlightTimerService timerService;

    public static CircleTimerClient getInstance() {
        return INSTANCE;
    }

    public ZoneManager getZoneManager() {
        return zoneManager;
    }

    public FlightTimerService getTimerService() {
        return timerService;
    }

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        CircleConfigStore configStore = new CircleConfigStore();
        this.zoneManager = new ZoneManager(configStore);
        this.timerService = new FlightTimerService(zoneManager);

        CircleCommand.register(zoneManager, timerService);
        HudRenderCallback.EVENT.register(new CircleHudRenderer(zoneManager, timerService));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String worldKey = WorldKeyResolver.resolve(client);
            zoneManager.switchWorld(worldKey);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            timerService.flushRunningSegment();
            zoneManager.saveNow();
            zoneManager.clearWorld();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) {
                timerService.flushRunningSegment();
                zoneManager.saveNow();
                return;
            }

            String resolvedWorldKey = WorldKeyResolver.resolve(client);
            if (!resolvedWorldKey.equals(zoneManager.getCurrentWorldKey())) {
                timerService.flushRunningSegment();
                zoneManager.switchWorld(resolvedWorldKey);
            }

            timerService.onClientTick(client);
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            timerService.flushRunningSegment();
            zoneManager.saveNow();
        });

        LOGGER.info("CircleTimer client initialized");
    }
}
