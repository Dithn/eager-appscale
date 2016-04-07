package edu.ucsb.cs.roots.anomaly;

import com.google.common.base.Strings;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class Anomaly {

    private final String id;
    private final AnomalyDetector detector;
    private final long start;
    private final long end;
    private final String operation;
    private final String description;

    Anomaly(AnomalyDetector detector, long start, long end, String operation, String description) {
        checkNotNull(detector, "Detector is required");
        checkArgument(start > 0 && end > 0 && start < end, "Time interval is invalid");
        checkArgument(!Strings.isNullOrEmpty(operation), "Operation is required");
        checkArgument(!Strings.isNullOrEmpty(description), "Description is required");
        this.id = UUID.randomUUID().toString();
        this.detector = detector;
        this.start = start;
        this.end = end;
        this.operation = operation;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public String getApplication() {
        return detector.getApplication();
    }

    public String getOperation() {
        return operation;
    }

    public String getDataStore() {
        return detector.getDataStore();
    }

    public int getPeriodInSeconds() {
        return detector.getPeriodInSeconds();
    }

    public String getDescription() {
        return description;
    }

    public String getDetectorProperty(String key, String def) {
        return detector.getProperty(key, def);
    }
}
