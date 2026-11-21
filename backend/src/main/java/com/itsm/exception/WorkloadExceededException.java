package com.itsm.exception;

/** Assigning would push the technician past their configured maximum workload. */
public class WorkloadExceededException extends RuntimeException {
    public WorkloadExceededException(String technicianName, int max) {
        super(technicianName + " already has the maximum of " + max + " active incidents");
    }
}
