package com.itsm.strategy;

/** Common contract for every pluggable strategy so they can be listed and switched at runtime. */
public interface NamedStrategy {

    /** Stable identifier used in the API and stored as the active choice. */
    String getKey();

    String getDisplayName();

    String getDescription();
}
