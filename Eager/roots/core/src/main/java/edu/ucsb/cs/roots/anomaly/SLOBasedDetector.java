package edu.ucsb.cs.roots.anomaly;

import com.google.common.collect.*;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.data.BenchmarkResult;
import edu.ucsb.cs.roots.data.DataStore;
import edu.ucsb.cs.roots.data.DataStoreException;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

public final class SLOBasedDetector extends AnomalyDetector {

    private final int samplingIntervalInSeconds;
    private final ListMultimap<String,BenchmarkResult> history;
    private final int responseTimeUpperBound;
    private final double sloPercentage;
    private final double windowFillPercentage;

    private long end = -1L;

    private SLOBasedDetector(RootsEnvironment environment, Builder builder) {
        super(environment, builder);
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
        this.samplingIntervalInSeconds = builder.samplingIntervalInSeconds;
        this.history = ArrayListMultimap.create();
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
        history.values().removeIf(l -> l.getTimestamp() < cutoff);

        int maxSamples = historyLengthInSeconds / samplingIntervalInSeconds;
        history.keySet().stream()
                .filter(op -> requestTypes.contains(op) &&
                        history.get(op).size() >= maxSamples * windowFillPercentage/100.0)
                .forEach(op -> computeSLO(cutoff, end, op, history.get(op)));
    }

    private Collection<String> updateHistory(long windowStart,
                                             long windowEnd) throws DataStoreException {
        checkArgument(windowStart < windowEnd, "Start time must precede end time");
        if (log.isDebugEnabled()) {
            log.debug("Updating history for {} ({} - {})", application, new Date(windowStart),
                    new Date(windowEnd));
        }
        DataStore ds = environment.getDataStoreService().get(this.dataStore);
        ImmutableListMultimap<String,BenchmarkResult> summaries =
                ds.getBenchmarkResults(application, windowStart, windowEnd);
        history.putAll(summaries);
        return ImmutableList.copyOf(summaries.keySet());
    }

    private void computeSLO(long start, long end, String operation,
                            Collection<BenchmarkResult> results) {
        if (isWaiting(operation, end)) {
            log.debug("Wait period in progress for {}:{}", application, operation);
            return;
        }

        log.debug("Calculating SLO with {} data points.", results.size());
        long satisfied = results.stream()
                .filter(r -> r.getResponseTime() <= responseTimeUpperBound)
                .count();
        double sloSupported = satisfied * 100.0 / results.size();
        log.info("SLO metrics. Supported: {}, Expected: {}", sloSupported, sloPercentage);
        if (sloSupported < sloPercentage) {
            Anomaly anomaly = Anomaly.newBuilder()
                    .setDetector(this)
                    .setStart(start)
                    .setEnd(end)
                    .setType(Anomaly.TYPE_PERFORMANCE)
                    .setOperation(operation)
                    .setDescription(String.format("SLA satisfaction: %.4f", sloSupported))
                    .build();
            reportAnomaly(anomaly);
        }
    }

    @Override
    protected long getWaitDuration(String operation) {
        // TODO: Parameterize this calculation from detector properties
        double minutes = Math.log(0.5)/Math.log(0.95);
        return (long) minutes * 60L * 1000L;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends AnomalyDetectorBuilder<SLOBasedDetector, Builder> {

        private int samplingIntervalInSeconds = 60;
        private int responseTimeUpperBound;
        private double sloPercentage = 95.0;
        private double windowFillPercentage = 95.0;

        private Builder() {
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
        public SLOBasedDetector build(RootsEnvironment environment) {
            return new SLOBasedDetector(environment, this);
        }

        @Override
        protected Builder getThisObj() {
            return this;
        }
    }
}
