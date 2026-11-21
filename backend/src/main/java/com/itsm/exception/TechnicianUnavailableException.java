package com.itsm.exception;

/** The chosen technician is marked unavailable or is inactive. */
public class TechnicianUnavailableException extends RuntimeException {
    public TechnicianUnavailableException(String technicianName) {
        super(technicianName + " is not available for new incidents right now");
    }
}
