package com.itsm.sla;

public enum SlaStatus {
    /** Open and comfortably inside the resolution window. */
    ON_TRACK,
    /** Open and more than 75% of the resolution window has elapsed. */
    AT_RISK,
    /** Open and past a deadline (response or resolution). */
    BREACHED,
    /** Resolved within the resolution deadline. */
    MET,
    /** Resolved, but after the resolution deadline. */
    MISSED
}
