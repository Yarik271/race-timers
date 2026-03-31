package com.circletimer.client.state;

import net.minecraft.util.math.BlockPos;

public class ZoneData {
    private static final double FLAT_HEIGHT_EPSILON = 1.0d;
    public int id;
    public double minX;
    public double minY;
    public double minZ;
    public double maxX;
    public double maxY;
    public double maxZ;
    public String name;

    public ZoneData() {
    }

    public ZoneData(int id, double x1, double y1, double z1, double x2, double y2, double z2) {
        this.id = id;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public boolean contains(BlockPos pos) {
        return contains(pos.getX(), pos.getY(), pos.getZ());
    }

    public boolean contains(double x, double y, double z) {
        boolean inXZ = x >= minX && x <= maxX
            && z >= minZ && z <= maxZ;
        if (!inXZ) {
            return false;
        }
        // Flat zone works as 2D trigger plane.
        if (isFlatTrigger()) {
            return true;
        }
        return y >= minY && y <= maxY;
    }

    public boolean isFlatTrigger() {
        return Math.abs(maxY - minY) < FLAT_HEIGHT_EPSILON;
    }
}
