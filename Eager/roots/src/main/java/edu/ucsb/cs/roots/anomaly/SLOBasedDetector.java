package edu.ucsb.cs.roots.anomaly;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import edu.ucsb.cs.roots.data.AccessLogEntry;
import edu.ucsb.cs.roots.utils.ImmutableCollectors;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;

public final class SLOBasedDetector extends AnomalyDetector {

    private final int historyLengthInSeconds;
    private final int samplingIntervalInSeconds;
    private final Map<String,List<AccessLogEntry>> history;
    private final int responseTimeUpperBound;
    private final double sloPercentage;

    private long end = -1L;

    private SLOBasedDetector(Builder builder) {
        super(builder.application, builder.periodInSeconds, builder.dataStore);
        checkArgument(builder.historyLengthInSeconds > 0, "History length must be positive");
        checkArgument(builder.samplingIntervalInSeconds > 0, "Sampling interval must be positive");
        checkArgument(builder.historyLengthInSeconds > builder.samplingIntervalInSeconds,
                "History length must be larger than sampling interval");
        checkArgument(builder.responseTimeUpperBound > 0,
                "Response time upper bound must be positive");
        checkArgument(builder.sloPercentage > 0 && builder.sloPercentage < 100,
                "SLO percentage must be in the interval (0,100)");
        this.historyLengthInSeconds = builder.historyLengthInSeconds;
        this.samplingIntervalInSeconds = builder.samplingIntervalInSeconds;
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

        ImmutableMap<String,ImmutableList<AccessLogEntry>> summaries =
                dataStore.getBenchmarkResults(application, start, end);
        for (Map.Entry<String,ImmutableList<AccessLogEntry>> entry : summaries.entrySet()) {
            List<AccessLogEntry> record = history.get(entry.getKey());
            if (record == null) {
                record = new ArrayList<>();
                history.put(entry.getKey(), record);
            }
            record.addAll(entry.getValue());
        }

        long cutoff = end - historyLengthInSeconds * 1000;
        history.values().forEach(v -> cleanupOldData(cutoff, v));

        int maxSamples = historyLengthInSeconds / samplingIntervalInSeconds;
        history.entrySet().stream()
                .filter(e -> summaries.containsKey(e.getKey()) &&
                        e.getValue().size() > maxSamples * 0.95)
                .forEach(e -> computeSLO(e.getKey(), e.getValue()));
    }

    private void cleanupOldData(long cutoff, List<AccessLogEntry> summaries) {
        ImmutableList<AccessLogEntry> oldData = summaries.stream()
                .filter(s -> s.getTimestamp() < cutoff)
                .collect(ImmutableCollectors.toList());
        oldData.forEach(summaries::remove);
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

        private int historyLengthInSeconds = 60 * 60;
        private int samplingIntervalInSeconds = 60;
        private int responseTimeUpperBound;
        private double sloPercentage = 95.0;

        private Builder() {
        }

        public Builder setHistoryLengthInSeconds(int historyLengthInSeconds) {
            this.historyLengthInSeconds = historyLengthInSeconds;
            return this;
        }

        public Builder setSamplingIntervalInSeconds(int samplingIntervalInSeconds) {
            this.samplingIntervalInSeconds = samplingIntervalInSeconds;
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
