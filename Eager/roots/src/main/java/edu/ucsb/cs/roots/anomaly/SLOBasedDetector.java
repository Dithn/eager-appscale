package edu.ucsb.cs.roots.anomaly;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableMap;
import edu.ucsb.cs.roots.data.AccessLogEntry;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

public final class SLOBasedDetector extends AnomalyDetector {

    private final int historyLength;
    private final Map<String,EvictingQueue<AccessLogEntry>> history;
    private final int responseTimeUpperBound;
    private final double sloPercentage;

    private long end = -1L;

    private SLOBasedDetector(Builder builder) {
        super(builder.application, builder.periodInSeconds, builder.dataStore);
        checkArgument(builder.historyLength > 10, "History length must be greater than 10");
        checkArgument(builder.responseTimeUpperBound > 0,
                "Response time upper bound must be positive");
        checkArgument(builder.sloPercentage > 0 && builder.sloPercentage < 100,
                "SLO percentage must be in the interval (0,100)");
        this.historyLength = builder.historyLength;
        this.history = new HashMap<>();
        this.responseTimeUpperBound = builder.responseTimeUpperBound;
        this.sloPercentage = builder.sloPercentage;
    }

    @Override
    public void run() {
        long start;
        if (end < 0) {
            end = System.currentTimeMillis() - 60 * 1000;
            start = end - periodInSeconds * 1000;
        } else {
            start = end;
            end += periodInSeconds * 1000;
        }

        ImmutableMap<String,List<AccessLogEntry>> summaries = dataStore.getBenchmarkResults(
                application, start, end);
        for (Map.Entry<String,List<AccessLogEntry>> entry : summaries.entrySet()) {
            EvictingQueue<AccessLogEntry> record = history.get(entry.getKey());
            if (record == null) {
                record = EvictingQueue.create(historyLength);
                history.put(entry.getKey(), record);
            }
            record.addAll(entry.getValue());
        }

        history.entrySet().stream()
                .filter(e -> summaries.containsKey(e.getKey()) &&
                        e.getValue().size() == historyLength)
                .forEach(e -> computeSLO(e.getKey(), e.getValue()));
    }

    private void computeSLO(String key, Collection<AccessLogEntry> logEntries) {
        long satisfied = logEntries.stream()
                .filter(e -> e.getResponseTime() <= responseTimeUpperBound)
                .count();
        double sloSupported = satisfied * 100.0 / logEntries.size();
        if (sloSupported < sloPercentage) {
            log.warn("Anomaly detected in {} -- SLA Satisfaction: {}%", key, sloSupported);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends AnomalyDetectorBuilder<SLOBasedDetector, Builder> {

        private int historyLength = 60;
        private int responseTimeUpperBound;
        private double sloPercentage = 95.0;

        private Builder() {
        }

        public Builder setHistoryLength(int historyLength) {
            this.historyLength = historyLength;
            return this;
        }

        public Builder setResponseTimeUpperBound(int responseTimeUpperBound) {
            this.responseTimeUpperBound = responseTimeUpperBound;
            return this;
        }

        public Builder setSloPercentage(double sloPercentage) {
            this.sloPercentage = sloPercentage;
            return this;
        }

        @Override
        public SLOBasedDetector build() {
            return new SLOBasedDetector(this);
        }

        @Override
        protected Builder getThisObj() {
            return this;
        }
    }
}
