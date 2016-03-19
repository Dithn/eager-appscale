package edu.ucsb.cs.roots.anomaly;

import edu.ucsb.cs.roots.data.AccessLogEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Based on "Detection of Performance Anomalies in Web-based Applications" by Magalhaes and Silva.
 * Calculates the correlation between response time and number of requests to detect any
 * anomalous increases in response time.
 */
public class CorrelationBasedAnomalyDetector extends AnomalyDetector {

    private final int historyLength;
    private final List<Summary> history;

    private long start = -1L;
    private long end = -1L;

    private CorrelationBasedAnomalyDetector(Builder builder) {
        super(builder.application, builder.period, builder.timeUnit, builder.dataStore);
        checkArgument(builder.historyLength > 10, "History length must be greater than 10");
        this.historyLength = builder.historyLength;
        this.history = new ArrayList<>(this.historyLength);
    }

    @Override
    public void run() {
        if (end < 0) {
            end = System.currentTimeMillis() - 60 * 1000;
            start = end - timeUnit.toMillis(period);
        } else {
            start = end;
            end += timeUnit.toMillis(period);
        }
        AccessLogEntry[] entries = dataStore.getAccessLogEntries(application, start, end);
        if (history.size() == historyLength) {
            history.remove(0);
        }
        history.add(new Summary(entries));
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    private static class Summary {
        private final double responseTime;
        private final double requestCount;

        private Summary(AccessLogEntry[] entries) {
            if (entries.length > 0) {
                responseTime = Arrays.asList(entries).stream()
                        .mapToDouble(AccessLogEntry::getResponseTime)
                        .average()
                        .getAsDouble();
            } else {
                responseTime = 0.0;
            }
            requestCount = entries.length;
        }
    }

    public static class Builder extends AnomalyDetectorBuilder<CorrelationBasedAnomalyDetector,Builder> {

        private int historyLength = 60;

        private Builder() {
        }

        @Override
        protected Builder getThisObj() {
            return this;
        }

        public Builder setHistoryLength(int historyLength) {
            this.historyLength = historyLength;
            return this;
        }

        @Override
        public CorrelationBasedAnomalyDetector build() {
            return new CorrelationBasedAnomalyDetector(this);
        }
    }
}
