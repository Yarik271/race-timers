package com.circletimer.client.hud;

import com.circletimer.client.state.HudSettingsData;
import com.circletimer.client.timer.FlightTimerService;
import com.circletimer.client.util.TimeFormat;
import com.circletimer.client.zone.ZoneManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

import java.util.Locale;

public class CircleHudRenderer implements HudRenderCallback {
    private final ZoneManager zoneManager;
    private final FlightTimerService timerService;

    public CircleHudRenderer(ZoneManager zoneManager, FlightTimerService timerService) {
        this.zoneManager = zoneManager;
        this.timerService = timerService;
    }

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        HudSettingsData hud = zoneManager.getHudSettings();
        if (!hud.visible) {
            return;
        }

        TextRenderer tr = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int anchorX = anchorX(screenWidth, hud);
        int topY = hud.offsetY;

        int target = zoneManager.getTargetLapCount();
        int currentCount = zoneManager.getCurrentZoneCount();
        String circlesText = Text.translatable("hud.circletimer.zones", currentCount, target).getString();

        Integer startId = zoneManager.getStartZoneId();
        if (zoneManager.getZones().isEmpty() || startId == null) {
            renderNoStart(drawContext, tr, hud, anchorX, topY, circlesText);
            return;
        }

        String startText = Text.translatable("hud.circletimer.start", startId, timerService.getCurrentCheckpointFromZero(), target).getString();
        String totalText = Text.translatable("hud.circletimer.total", TimeFormat.format(timerService.getTotalMillis())).getString();
        String bestText = Text.translatable("hud.circletimer.best", TimeFormat.format(timerService.getBestLapMillis())).getString();
        String currentText = Text.translatable("hud.circletimer.current", TimeFormat.format(timerService.getCurrentLapMillis())).getString();

        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().scale((float) hud.scale, (float) hud.scale);

        int scaledAnchorX = (int) (anchorX / hud.scale);
        int y = (int) (topY / hud.scale);

        int lineStep = tr.fontHeight + 2;
        int totalHeight = tr.fontHeight * 2 + 2;
        int maxNormalWidth = Math.max(tr.getWidth(circlesText), Math.max(tr.getWidth(startText), Math.max(tr.getWidth(bestText), tr.getWidth(currentText))));
        int totalWidth = tr.getWidth(totalText) * 2;
        int maxWidth = Math.max(maxNormalWidth, totalWidth);

        drawBlockBackground(drawContext, hud, scaledAnchorX, y, maxWidth, lineStep, totalHeight);

        drawAligned(tr, drawContext, hud, circlesText, scaledAnchorX, y, hud.colorLabel);
        y += lineStep;

        drawAligned(tr, drawContext, hud, startText, scaledAnchorX, y, hud.colorLabel);
        y += lineStep;

        drawAlignedScaled(tr, drawContext, hud, totalText, scaledAnchorX, y, hud.colorTotal, 2.0f);
        y += totalHeight;

        drawAligned(tr, drawContext, hud, bestText, scaledAnchorX, y, hud.colorBest);
        y += lineStep;
        drawAligned(tr, drawContext, hud, currentText, scaledAnchorX, y, hud.colorCurrent);

        drawContext.getMatrices().popMatrix();
    }

    private void renderNoStart(DrawContext drawContext, TextRenderer tr, HudSettingsData hud, int anchorX, int topY, String circlesText) {
        String noStartText = Text.translatable("hud.circletimer.no_start").getString();

        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().scale((float) hud.scale, (float) hud.scale);

        int scaledAnchorX = (int) (anchorX / hud.scale);
        int y = (int) (topY / hud.scale);
        int lineStep = tr.fontHeight + 2;
        int maxWidth = Math.max(tr.getWidth(circlesText), tr.getWidth(noStartText));

        if (hud.backgroundAlpha > 0) {
            int padding = 4;
            int height = lineStep * 2 + padding * 2 - 2;
            int leftX = alignedLeftX(hud, scaledAnchorX, maxWidth + padding * 2);
            drawContext.fill(leftX, y - padding, leftX + maxWidth + padding * 2, y + height, (hud.backgroundAlpha << 24) | 0x202020);
        }

        drawAligned(tr, drawContext, hud, circlesText, scaledAnchorX, y, hud.colorLabel);
        drawAligned(tr, drawContext, hud, noStartText, scaledAnchorX, y + lineStep, hud.colorLabel);

        drawContext.getMatrices().popMatrix();
    }

    private static int anchorX(int screenWidth, HudSettingsData hud) {
        String align = hud.horizontalAlign == null ? "right" : hud.horizontalAlign.toLowerCase(Locale.ROOT);
        if ("left".equals(align)) {
            return hud.offsetX;
        }
        if ("center".equals(align)) {
            return screenWidth / 2 + hud.offsetX;
        }
        return screenWidth - hud.offsetX;
    }

    private static void drawAligned(TextRenderer tr, DrawContext dc, HudSettingsData hud, String text, int anchorX, int y, int color) {
        int x = alignedTextX(hud, anchorX, tr.getWidth(text));
        int opaqueColor = 0xFF000000 | (color & 0x00FFFFFF);
        dc.drawText(tr, text, x, y, opaqueColor, true);
    }

    private static void drawAlignedScaled(TextRenderer tr, DrawContext dc, HudSettingsData hud, String text, int anchorX, int y, int color, float scale) {
        dc.getMatrices().pushMatrix();
        dc.getMatrices().scale(scale, scale);

        int scaledAnchor = (int) (anchorX / scale);
        int scaledY = (int) (y / scale);
        drawAligned(tr, dc, hud, text, scaledAnchor, scaledY, color);

        dc.getMatrices().popMatrix();
    }

    private static int alignedTextX(HudSettingsData hud, int anchorX, int width) {
        String align = hud.horizontalAlign == null ? "right" : hud.horizontalAlign.toLowerCase(Locale.ROOT);
        if ("left".equals(align)) {
            return anchorX;
        }
        if ("center".equals(align)) {
            return anchorX - (width / 2);
        }
        return anchorX - width;
    }

    private static int alignedLeftX(HudSettingsData hud, int anchorX, int width) {
        String align = hud.horizontalAlign == null ? "right" : hud.horizontalAlign.toLowerCase(Locale.ROOT);
        if ("left".equals(align)) {
            return anchorX;
        }
        if ("center".equals(align)) {
            return anchorX - (width / 2);
        }
        return anchorX - width;
    }

    private static void drawBlockBackground(DrawContext dc, HudSettingsData hud, int anchorX, int y, int maxWidth, int lineStep, int totalHeight) {
        if (hud.backgroundAlpha <= 0) {
            return;
        }
        int padding = 4;
        int width = maxWidth + padding * 2;
        int height = lineStep * 2 + totalHeight + lineStep * 2 + padding * 2 - 2;
        int leftX = alignedLeftX(hud, anchorX, width);
        dc.fill(leftX, y - padding, leftX + width, y + height, (hud.backgroundAlpha << 24) | 0x202020);
    }
}