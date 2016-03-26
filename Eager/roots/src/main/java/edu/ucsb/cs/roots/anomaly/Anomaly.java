package edu.ucsb.cs.roots.anomaly;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class Anomaly {

    private final long start;
    private final long end;
    private final String application;
    private final String operation;
    private final String dataStore;
    private final int periodInSeconds;
    private final String description;

    Anomaly(AnomalyDetector detector, long start, long end, String operation, String description) {
        checkNotNull(detector, "Detector is required");
        checkArgument(start > 0 && end > 0 && start < end, "Time interval is invalid");
        checkArgument(!Strings.isNullOrEmpty(operation), "Operation is required");
        checkArgument(!Strings.isNullOrEmpty(description), "Description is required");
        this.start = start;
        this.end = end;
        this.application = detector.getApplication();
        this.operation = operation;
        this.dataStore = detector.getDataStore();
        this.periodInSeconds = detector.getPeriodInSeconds();
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

    public String getDataStore() {
        return dataStore;
    }

    public int getPeriodInSeconds() {
        return periodInSeconds;
    }

    public String getDescription() {
        return description;
    }
}
