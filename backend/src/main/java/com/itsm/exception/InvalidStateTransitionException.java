package com.itsm.exception;

public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String currentState, String targetState) {
        super(String.format("Invalid state transition from %s to %s", currentState, targetState));
    }
}
