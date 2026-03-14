package com.circletimer.client.world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.util.Locale;

public final class WorldKeyResolver {
    private WorldKeyResolver() {
    }

    public static String resolve(MinecraftClient client) {
        ServerInfo serverInfo = client.getCurrentServerEntry();
        if (serverInfo != null && serverInfo.address != null && !serverInfo.address.isBlank()) {
            return "server:" + serverInfo.address.toLowerCase(Locale.ROOT);
        }

        if (client.getServer() != null) {
            String levelName = client.getServer().getSaveProperties().getLevelName();
            return "singleplayer:" + levelName.toLowerCase(Locale.ROOT);
        }

        return "unknown";
    }
}
