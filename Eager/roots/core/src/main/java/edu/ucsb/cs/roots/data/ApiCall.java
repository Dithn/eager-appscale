package edu.ucsb.cs.roots.data;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;

public final class ApiCall {

    private final long timestamp;
    private final String service;
    private final String operation;
    private final int timeElapsed;

    public ApiCall(long timestamp, String service, String operation, int timeElapsed) {
        checkArgument(timestamp > 0, "Timestamp must be positive");
        checkArgument(!Strings.isNullOrEmpty(service), "Service is required");
        checkArgument(!Strings.isNullOrEmpty(operation), "Operation is required");
        checkArgument(timeElapsed >= 0, "Time elapsed must be non-negative");
        this.timestamp = timestamp;
        this.service = service;
        this.operation = operation;
        this.timeElapsed = timeElapsed;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getService() {
        return service;
    }

    public String getOperation() {
        return operation;
    }

    public String name() {
        return service + ":" + operation;
    }

    public int getTimeElapsed() {
        return timeElapsed;
    }
}
