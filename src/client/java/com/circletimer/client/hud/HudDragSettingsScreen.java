package com.circletimer.client.hud;

import com.circletimer.client.state.HudSettingsData;
import com.circletimer.client.timer.FlightTimerService;
import com.circletimer.client.util.TimeFormat;
import com.circletimer.client.zone.ZoneManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.Locale;

public class HudDragSettingsScreen extends Screen {
    private final Screen parent;
    private final ZoneManager zoneManager;
    private final FlightTimerService timerService;

    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public HudDragSettingsScreen(Screen parent, ZoneManager zoneManager, FlightTimerService timerService) {
        super(Text.translatable("screen.circletimer.settings.title"));
        this.parent = parent;
        this.zoneManager = zoneManager;
        this.timerService = timerService;
    }

    @Override
    protected void init() {
        HudSettingsData hud = zoneManager.getHudSettings();
        int center = this.width / 2;
        int y = 18;

        addDrawableChild(ButtonWidget.builder(toggleText(hud), button -> {
            hud.visible = !hud.visible;
            zoneManager.markDirty();
            zoneManager.saveNow();
            button.setMessage(toggleText(hud));
        }).dimensions(center - 150, y, 300, 20).build());
        y += 24;

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.circletimer.settings.scale_minus"), button -> {
            hud.scale = Math.max(0.5d, hud.scale - 0.1d);
            zoneManager.markDirty();
        }).dimensions(center - 150, y, 145, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.circletimer.settings.scale_plus"), button -> {
            hud.scale = Math.min(6.0d, hud.scale + 0.1d);
            zoneManager.markDirty();
        }).dimensions(center + 5, y, 145, 20).build());
        y += 24;

        addDrawableChild(ButtonWidget.builder(alignText(hud), button -> {
            hud.horizontalAlign = nextAlign(hud.horizontalAlign);
            zoneManager.markDirty();
            zoneManager.saveNow();
            button.setMessage(alignText(hud));
        }).dimensions(center - 150, y, 145, 20).build());

        addDrawableChild(ButtonWidget.builder(backgroundText(hud), button -> {
            hud.backgroundAlpha = (hud.backgroundAlpha > 0) ? 0 : 127;
            zoneManager.markDirty();
            zoneManager.saveNow();
            button.setMessage(backgroundText(hud));
        }).dimensions(center + 5, y, 145, 20).build());
        y += 24;

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.circletimer.settings.reset_position"), button -> {
            HudSettingsData defaults = HudSettingsData.defaults();
            hud.offsetX = defaults.offsetX;
            hud.offsetY = defaults.offsetY;
            zoneManager.markDirty();
            zoneManager.saveNow();
        }).dimensions(center - 150, y, 145, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.circletimer.settings.reset_colors"), button -> {
            HudSettingsData defaults = HudSettingsData.defaults();
            hud.colorLabel = defaults.colorLabel;
            hud.colorTotal = defaults.colorTotal;
            hud.colorBest = defaults.colorBest;
            hud.colorCurrent = defaults.colorCurrent;
            zoneManager.markDirty();
            zoneManager.saveNow();
        }).dimensions(center + 5, y, 145, 20).build());
        y += 24;

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.circletimer.settings.done"), button -> close()).dimensions(center - 150, y, 300, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 4, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.circletimer.settings.hint"), this.width / 2, this.height - 18, 0xB0B0B0);

        renderHudPreview(context, mouseX, mouseY);
        zoneManager.maybeAutoSave(System.currentTimeMillis());
    }

    private void renderHudPreview(DrawContext context, int mouseX, int mouseY) {
        HudSettingsData hud = zoneManager.getHudSettings();

        int[] box = calculatePreviewBox();
        int left = box[0];
        int top = box[1];
        int right = box[2];
        int bottom = box[3];

        if (hud.backgroundAlpha > 0) {
            int alpha = isPointInside(mouseX, mouseY, left, top, right, bottom) ? Math.min(255, hud.backgroundAlpha + 40) : hud.backgroundAlpha;
            context.fill(left - 4, top - 4, right + 4, bottom + 4, (alpha << 24) | 0x202020);
        }

        String line1 = Text.translatable("hud.circletimer.start", 1, timerService.getCurrentCheckpointFromZero(), zoneManager.getTargetLapCount()).getString();
        String line2 = Text.translatable("hud.circletimer.total", TimeFormat.format(timerService.getTotalMillis())).getString();
        String line3 = Text.translatable("hud.circletimer.best", TimeFormat.format(timerService.getBestLapMillis())).getString();
        String line4 = Text.translatable("hud.circletimer.current", TimeFormat.format(timerService.getCurrentLapMillis())).getString();

        float baseScale = (float) hud.scale;
        int lineH = this.textRenderer.fontHeight + 2;
        int totalHeight = this.textRenderer.fontHeight * 2 + 2;

        int anchorX = getAnchorX(hud);
        Object stack = MatrixCompat.getStack(context);
        MatrixCompat.push(stack);
        MatrixCompat.scale(stack, baseScale, baseScale);

        int scaledAnchorX = (int) (anchorX / baseScale);
        int y = (int) (hud.offsetY / baseScale);

        drawAligned(context, line1, scaledAnchorX, y, hud.colorLabel, hud);
        y += lineH;

        Object totalStack = MatrixCompat.getStack(context);
        MatrixCompat.push(totalStack);
        MatrixCompat.scale(totalStack, 2.0f, 2.0f);
        drawAligned(context, line2, (int) (scaledAnchorX / 2.0f), (int) (y / 2.0f), hud.colorTotal, hud);
        MatrixCompat.pop(totalStack);
        y += totalHeight;

        drawAligned(context, line3, scaledAnchorX, y, hud.colorBest, hud);
        y += lineH;
        drawAligned(context, line4, scaledAnchorX, y, hud.colorCurrent, hud);

        MatrixCompat.pop(stack);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0) {
            return false;
        }

        int[] box = calculatePreviewBox();
        if (isPointInside((int) mouseX, (int) mouseY, box[0], box[1], box[2], box[3])) {
            dragging = true;
            dragOffsetX = (int) mouseX - box[0];
            dragOffsetY = (int) mouseY - box[1];
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!dragging || button != 0) {
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        HudSettingsData hud = zoneManager.getHudSettings();
        int[] box = calculatePreviewBox();
        int widthPx = box[2] - box[0];

        int newLeft = (int) mouseX - dragOffsetX;
        int newTop = (int) mouseY - dragOffsetY;

        hud.offsetY = clamp(newTop, 0, Math.max(0, this.height - 20));

        String align = normalizeAlign(hud.horizontalAlign);
        if ("left".equals(align)) {
            hud.offsetX = clamp(newLeft, 0, Math.max(0, this.width - 20));
        } else if ("center".equals(align)) {
            int centerAnchor = newLeft + (widthPx / 2);
            hud.offsetX = clamp(centerAnchor - (this.width / 2), -this.width / 2, this.width / 2);
        } else {
            int right = newLeft + widthPx;
            hud.offsetX = clamp(this.width - right, 0, Math.max(0, this.width - 20));
        }

        zoneManager.markDirty();
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;
            zoneManager.saveNow();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        zoneManager.saveNow();
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    private int[] calculatePreviewBox() {
        HudSettingsData hud = zoneManager.getHudSettings();

        String line1 = Text.translatable("hud.circletimer.start", 1, 0, zoneManager.getTargetLapCount()).getString();
        String line2 = Text.translatable("hud.circletimer.total", "00:00.00").getString();
        String line3 = Text.translatable("hud.circletimer.best", "00:00.00").getString();
        String line4 = Text.translatable("hud.circletimer.current", "00:00.00").getString();

        int maxNormalWidth = Math.max(this.textRenderer.getWidth(line1), Math.max(this.textRenderer.getWidth(line3), this.textRenderer.getWidth(line4)));
        int totalWidth = this.textRenderer.getWidth(line2) * 2;
        int contentWidth = Math.max(maxNormalWidth, totalWidth);

        int lineH = this.textRenderer.fontHeight + 2;
        int totalHeight = this.textRenderer.fontHeight * 2 + 2;
        int contentHeight = lineH + totalHeight + lineH + lineH;

        int scaledW = Math.round((float) (contentWidth * hud.scale));
        int scaledH = Math.round((float) (contentHeight * hud.scale));

        int anchorX = getAnchorX(hud);
        int left;
        String align = normalizeAlign(hud.horizontalAlign);
        if ("left".equals(align)) {
            left = anchorX;
        } else if ("center".equals(align)) {
            left = anchorX - (scaledW / 2);
        } else {
            left = anchorX - scaledW;
        }

        return new int[]{left, hud.offsetY, left + scaledW, hud.offsetY + scaledH};
    }

    private int getAnchorX(HudSettingsData hud) {
        String align = normalizeAlign(hud.horizontalAlign);
        if ("left".equals(align)) {
            return hud.offsetX;
        }
        if ("center".equals(align)) {
            return (this.width / 2) + hud.offsetX;
        }
        return this.width - hud.offsetX;
    }

    private void drawAligned(DrawContext context, String text, int anchorX, int y, int color, HudSettingsData hud) {
        int width = this.textRenderer.getWidth(text);
        int x;
        String align = normalizeAlign(hud.horizontalAlign);
        if ("left".equals(align)) {
            x = anchorX;
        } else if ("center".equals(align)) {
            x = anchorX - (width / 2);
        } else {
            x = anchorX - width;
        }
        context.drawText(this.textRenderer, text, x, y, 0xFF000000 | (color & 0x00FFFFFF), true);
    }

    private static String nextAlign(String current) {
        String align = normalizeAlign(current);
        if ("right".equals(align)) {
            return "left";
        }
        if ("left".equals(align)) {
            return "center";
        }
        return "right";
    }

    private static String normalizeAlign(String align) {
        if (align == null) {
            return "right";
        }
        String low = align.toLowerCase(Locale.ROOT);
        if ("left".equals(low) || "center".equals(low) || "right".equals(low)) {
            return low;
        }
        return "right";
    }

    private static Text toggleText(HudSettingsData hud) {
        return Text.translatable("screen.circletimer.settings.visible", hud.visible ? Text.translatable("screen.circletimer.settings.on") : Text.translatable("screen.circletimer.settings.off"));
    }

    private static Text alignText(HudSettingsData hud) {
        String align = normalizeAlign(hud.horizontalAlign);
        return Text.translatable("screen.circletimer.settings.align", Text.translatable("screen.circletimer.settings.align." + align));
    }

    private static Text backgroundText(HudSettingsData hud) {
        return Text.translatable("screen.circletimer.settings.background", hud.backgroundAlpha > 0 ? Text.translatable("screen.circletimer.settings.background.semi") : Text.translatable("screen.circletimer.settings.background.off"));
    }

    private static boolean isPointInside(int x, int y, int left, int top, int right, int bottom) {
        return x >= left && x <= right && y >= top && y <= bottom;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
