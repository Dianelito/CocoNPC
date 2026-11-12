package com.pumpkings.coconpc.core.mob.model;

/**
 * The six faces of a cube, in the order used by both Minecraft box UVs and the
 * player-head texture layout. The {@code skinX}/{@code skinY} coordinates point
 * at the 8x8 region of a standard 64x64 player skin that the head model samples
 * for that face.
 */
public enum FaceDirection {
    DOWN("down", 16, 0),
    UP("up", 8, 0),
    EAST("east", 0, 8),
    NORTH("north", 8, 8),
    WEST("west", 16, 8),
    SOUTH("south", 24, 8);

    private final String key;
    private final int skinX;
    private final int skinY;

    FaceDirection(String key, int skinX, int skinY) {
        this.key = key;
        this.skinX = skinX;
        this.skinY = skinY;
    }

    public String getKey() {
        return key;
    }

    public int getSkinX() {
        return skinX;
    }

    public int getSkinY() {
        return skinY;
    }

    public static FaceDirection fromKey(String key) {
        if (key == null) return null;
        String normalized = key.toLowerCase();
        for (FaceDirection direction : values()) {
            if (direction.key.equals(normalized)) return direction;
        }
        return null;
    }

    /**
     * Automatic Minecraft box UV rectangle for a cube of the given size, using
     * the texture-space offset (0,0). Returns {u1, v1, u2, v2} in pixels.
     */
    public float[] defaultUv(float width, float height, float depth) {
        float d = depth;
        float w = width;
        float h = height;
        return switch (this) {
            case UP -> new float[]{d, 0, d + w, d};
            case DOWN -> new float[]{d + w, 0, d + w + w, d};
            case EAST -> new float[]{0, d, d, d + h};
            case NORTH -> new float[]{d, d, d + w, d + h};
            case WEST -> new float[]{d + w, d, d + w + d, d + h};
            case SOUTH -> new float[]{d + w + d, d, d + w + d + w, d + h};
        };
    }

    public static FaceDirection[] ordered() {
        return new FaceDirection[]{DOWN, UP, WEST, NORTH, EAST, SOUTH};
    }
}
