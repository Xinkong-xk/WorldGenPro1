package io.xk.worldgenpro.generator.structure.core.api;

public enum Dir {
    UP, RIGHT, DOWN, LEFT;

    public static Dir opposite(Dir d) {
        return switch (d) {
            case UP -> Dir.DOWN;
            case DOWN -> Dir.UP;
            case LEFT -> Dir.RIGHT;
            case RIGHT -> Dir.LEFT;
            default -> throw new IllegalArgumentException();
        };
    }

    public Dir opposite() {
        return switch (this) {
            case UP -> Dir.DOWN;
            case DOWN -> Dir.UP;
            case LEFT -> Dir.RIGHT;
            case RIGHT -> Dir.LEFT;
            default -> throw new IllegalArgumentException();
        };
    }
}