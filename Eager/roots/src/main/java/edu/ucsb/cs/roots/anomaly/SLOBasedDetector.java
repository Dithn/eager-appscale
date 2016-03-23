package edu.ucsb.cs.roots.anomaly;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import edu.ucsb.cs.roots.config.DataStoreManager;
import edu.ucsb.cs.roots.data.AccessLogEntry;
import edu.ucsb.cs.roots.data.DataStore;
import edu.ucsb.cs.roots.data.DataStoreException;
import edu.ucsb.cs.roots.utils.ImmutableCollectors;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;

public final class SLOBasedDetector extends AnomalyDetector {

    private final int historyLengthInSeconds;
    private final int samplingIntervalInSeconds;
    private final Map<String,List<AccessLogEntry>> history;
    private final int responseTimeUpperBound;
    private final double sloPercentage;
    private final double windowFillPercentage;

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
        checkArgument(builder.windowFillPercentage > 0 && builder.windowFillPercentage <= 100,
                "Window fill percentage must be in the interval (0,100]");
        this.historyLengthInSeconds = builder.historyLengthInSeconds;
        this.samplingIntervalInSeconds = builder.samplingIntervalInSeconds;
        this.history = new HashMap<>();
        this.responseTimeUpperBound = builder.responseTimeUpperBound;
        this.sloPercentage = builder.sloPercentage;
        this.windowFillPercentage = builder.windowFillPercentage;
    }

    @Override
    public void run(long now) {
        Collection<String> requestTypes;
        try {
            long tempStart, tempEnd;
            if (end < 0) {
                tempEnd = now - 60 * 1000;
                tempStart = tempEnd - historyLengthInSeconds * 1000;
            } else {
                tempStart = end;
                tempEnd = end + periodInSeconds * 1000;
            }

            requestTypes = updateHistory(tempStart, tempEnd);
            end = tempEnd;
        } catch (DataStoreException e) {
            String msg = "Error while retrieving data";
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }

        long cutoff = end - historyLengthInSeconds * 1000;
        history.values().forEach(v -> cleanupOldData(cutoff, v));

        int maxSamples = historyLengthInSeconds / samplingIntervalInSeconds;
        history.entrySet().stream()
                .filter(e -> requestTypes.contains(e.getKey()) &&
                        e.getValue().size() >= maxSamples * windowFillPercentage)
                .forEach(e -> computeSLO(e.getKey(), e.getValue()));
    }

    private Collection<String> updateHistory(long windowStart,
                                             long windowEnd) throws DataStoreException {
        checkArgument(windowStart < windowEnd, "Start time must precede end time");
        DataStore ds = DataStoreManager.getInstance().get(this.dataStore);
        ImmutableMap<String,ImmutableList<AccessLogEntry>> summaries =
                ds.getBenchmarkResults(application, windowStart, windowEnd);
        for (Map.Entry<String,ImmutableList<AccessLogEntry>> entry : summaries.entrySet()) {
            List<AccessLogEntry> record = history.get(entry.getKey());
            if (record == null) {
                record = new ArrayList<>();
                history.put(entry.getKey(), record);
            }
            record.addAll(entry.getValue());
        }
        return ImmutableList.copyOf(summaries.keySet());
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
        private double windowFillPercentage = 95.0;

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

        public Builder setWindowFillPercentage(double windowFillPercentage) {
            this.windowFillPercentage = windowFillPercentage;
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
