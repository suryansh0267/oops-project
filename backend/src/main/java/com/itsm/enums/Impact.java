package com.itsm.enums;

public enum Impact {
    HIGH(3),
    MEDIUM(2),
    LOW(1);

    private final int value;

    Impact(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
