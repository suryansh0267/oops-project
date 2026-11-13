package com.itsm.enums;

public enum Urgency {
    HIGH(3),
    MEDIUM(2),
    LOW(1);

    private final int value;

    Urgency(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
