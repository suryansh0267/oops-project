package com.itsm.state;

import com.itsm.enums.IncidentStatus;
import com.itsm.exception.InvalidStateTransitionException;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory + flyweight: one immutable state object per status.
 */
public final class IncidentStateFactory {

    private static final Map<IncidentStatus, IncidentState> STATES = new EnumMap<>(IncidentStatus.class);

    static {
        register(new NewState());
        register(new ClassifiedState());
        register(new AssignedState());
        register(new AcknowledgedState());
        register(new InProgressState());
        register(new ResolvedState());
        register(new VerifiedState());
        register(new ClosedState());
    }

    private IncidentStateFactory() {
    }

    private static void register(IncidentState state) {
        STATES.put(state.status(), state);
    }

    public static IncidentState of(IncidentStatus status) {
        IncidentState state = STATES.get(status == null ? IncidentStatus.NEW : status);
        if (state == null) {
            throw new IllegalStateException("No state object registered for " + status);
        }
        return state;
    }

    /** Throws InvalidStateTransitionException when the move is not allowed. */
    public static void assertTransition(IncidentStatus current, IncidentStatus target) {
        if (!of(current).canTransitionTo(target)) {
            throw new InvalidStateTransitionException(
                    current == null ? "NEW" : current.name(), target.name());
        }
    }

    public static List<IncidentStatus> openStatuses() {
        return Arrays.stream(IncidentStatus.values()).filter(s -> of(s).isOpen()).toList();
    }

    public static Map<IncidentStatus, IncidentState> all() {
        return Collections.unmodifiableMap(STATES);
    }
}
