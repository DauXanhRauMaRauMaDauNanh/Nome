package org.bukkit;

public enum GameMode {
    SURVIVAL(0),
    CREATIVE(1),
    ADVENTURE(2),
    SPECTATOR(3);

    private final int value;

    GameMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static GameMode getByValue(int value) {
        for (GameMode gm : values()) {
            if (gm.value == value) return gm;
        }
        return SURVIVAL;
    }
}
