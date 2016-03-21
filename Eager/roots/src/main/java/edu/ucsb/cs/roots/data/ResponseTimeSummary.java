package edu.ucsb.cs.roots.data;

import java.util.Collection;

public final class ResponseTimeSummary {

    private final double meanResponseTime;
    private final double requestCount;

    public ResponseTimeSummary(double meanResponseTime, double requestCount) {
        this.meanResponseTime = meanResponseTime;
        this.requestCount = requestCount;
    }

    public ResponseTimeSummary(Collection<AccessLogEntry> entries) {
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

    public double getMeanResponseTime() {
        return meanResponseTime;
    }

    public double getRequestCount() {
        return requestCount;
    }
}
