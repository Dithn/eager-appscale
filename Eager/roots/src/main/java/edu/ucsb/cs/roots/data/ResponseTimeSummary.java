package edu.ucsb.cs.roots.data;

import java.util.Collection;

public final class ResponseTimeSummary {

    private final long timestamp;
    private final double meanResponseTime;
    private final double requestCount;

    public ResponseTimeSummary(long timestamp, double meanResponseTime, double requestCount) {
        this.meanResponseTime = meanResponseTime;
        this.requestCount = requestCount;
        this.timestamp = timestamp;
    }

    public ResponseTimeSummary(long timestamp, Collection<AccessLogEntry> entries) {
        this.timestamp = timestamp;
        if (entries.size() > 0) {
            meanResponseTime = entries.stream()
                    .mapToDouble(AccessLogEntry::getResponseTime)
                    .average()
                    .getAsDouble();
        } else {
            meanResponseTime = 0.0;
        }
        requestCount = entries.size();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getMeanResponseTime() {
        return meanResponseTime;
    }

    public double getRequestCount() {
        return requestCount;
    }
}
