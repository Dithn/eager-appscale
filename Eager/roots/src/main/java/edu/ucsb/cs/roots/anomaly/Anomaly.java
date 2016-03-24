package edu.ucsb.cs.roots.anomaly;

public final class Anomaly {

    private final long timestamp;
    private final String application;
    private final String operation;
    private final String description;

    public Anomaly(long timestamp, String application, String operation, String description) {
        this.timestamp = timestamp;
        this.application = application;
        this.operation = operation;
        this.description = description;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getApplication() {
        return application;
    }

    public String getOperation() {
        return operation;
    }

    public String getDescription() {
        return description;
    }
}
