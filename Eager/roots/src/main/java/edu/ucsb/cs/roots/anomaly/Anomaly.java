package edu.ucsb.cs.roots.anomaly;

public final class Anomaly {

    private final long start;
    private final long end;
    private final String application;
    private final String operation;
    private final String description;

    public Anomaly(long start, long end, String application, String operation, String description) {
        this.start = start;
        this.end = end;
        this.application = application;
        this.operation = operation;
        this.description = description;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
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
