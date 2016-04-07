package edu.ucsb.cs.roots.anomaly;

import com.google.common.collect.*;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.data.DataStore;
import edu.ucsb.cs.roots.data.DataStoreException;
import edu.ucsb.cs.roots.data.ResponseTimeSummary;
import edu.ucsb.cs.roots.rlang.RClient;
import org.rosuda.REngine.REXP;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;

public final class CorrelationBasedDetector extends AnomalyDetector {

    private final ListMultimap<String,ResponseTimeSummary> history;
    private final double correlationThreshold;
    private final double dtwIncreaseThreshold;

    private long end = -1L;
    private Map<String,Double> prevDtw = new HashMap<>();

    private CorrelationBasedDetector(RootsEnvironment environment, Builder builder) {
        super(environment, builder.application, builder.periodInSeconds,
                builder.historyLengthInSeconds, builder.dataStore, builder.properties);
        checkArgument(builder.correlationThreshold >= -1 && builder.correlationThreshold <= 1,
                "Correlation threshold must be in the interval [-1,1]");
        checkArgument(builder.dtwIncreaseThreshold > 0,
                "DTW increase percentage threshold must be positive");
        this.history = ArrayListMultimap.create();
        this.correlationThreshold = builder.correlationThreshold;
        this.dtwIncreaseThreshold = builder.dtwIncreaseThreshold;
    }

    @Override
    public void run(long now) {
        Collection<String> requestTypes;
        try {
            long tempStart, tempEnd;
            if (end < 0) {
                tempEnd = now - 60 * 1000 - periodInSeconds * 1000;
                tempStart = tempEnd - historyLengthInSeconds * 1000;
                initFullHistory(tempStart, tempEnd);
                end = tempEnd;
            }
            tempStart = end;
            tempEnd = end + periodInSeconds * 1000;
            requestTypes = updateHistory(tempStart, tempEnd);
            end = tempEnd;
        } catch (DataStoreException e) {
            String msg = "Error while retrieving data";
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }

        long cutoff = end - historyLengthInSeconds * 1000;
        history.values().removeIf(s -> s.getTimestamp() < cutoff);
        history.keySet().stream()
                .filter(k -> requestTypes.contains(k) && history.get(k).size() > 2)
                .map(k -> computeCorrelation(k, history.get(k)))
                .filter(Objects::nonNull)
                .forEach(c -> checkForAnomalies(cutoff, end, c));
    }

    private void initFullHistory(long windowStart, long windowEnd) throws DataStoreException {
        checkArgument(windowStart < windowEnd, "Start time must precede end time");
        DataStore ds = environment.getDataStoreService().get(this.dataStore);
        ImmutableListMultimap<String,ResponseTimeSummary> summaries =
                ds.getResponseTimeHistory(application, windowStart, windowEnd,
                        periodInSeconds * 1000);
        history.putAll(summaries);
        history.keySet().stream()
                .filter(k -> history.get(k).size() > 2)
                .map(k -> computeCorrelation(k, history.get(k)))
                .filter(Objects::nonNull)
                .forEach(c -> prevDtw.put(c.key, c.dtw));
    }

    private Collection<String> updateHistory(long windowStart,
                                             long windowEnd) throws DataStoreException {
        checkArgument(windowStart < windowEnd, "Start time must precede end time");
        DataStore ds = environment.getDataStoreService().get(this.dataStore);
        ImmutableMap<String,ResponseTimeSummary> summaries = ds.getResponseTimeSummary(
                application, windowStart, windowEnd);
        summaries.forEach(history::put);
        return ImmutableList.copyOf(summaries.keySet());
    }

    private Correlation computeCorrelation(String key, List<ResponseTimeSummary> summaries) {
        double[] requests = new double[summaries.size()];
        double[] responseTime = new double[summaries.size()];
        for (int i = 0; i < summaries.size(); i++) {
            ResponseTimeSummary s = summaries.get(i);
            requests[i] = s.getRequestCount();
            responseTime[i] = s.getMeanResponseTime();
        }

        try (RClient r = new RClient(environment.getRService())) {
            r.assign("x", requests);
            r.assign("y", responseTime);
            REXP correlation = r.eval("cor(x, y, method='pearson')");
            r.evalAndAssign("time_warp", "dtw(x, y)");
            REXP distance = r.eval("time_warp$distance");
            log.info("Correlation analysis output [{}]: {} {} {}", key, correlation.asDouble(),
                    distance.asDouble(), requests.length);
            return new Correlation(key, correlation.asDouble(), distance.asDouble());
        } catch (Exception e) {
            log.error("Error computing the correlation statistics", e);
            return null;
        }
    }

    private void checkForAnomalies(long start, long end, Correlation correlation) {
        double lastDtw = prevDtw.getOrDefault(correlation.key, -1.0);
        if (correlation.rValue < correlationThreshold && lastDtw >= 0) {
            // If the correlation has dropped and the DTW distance has increased, we
            // might be looking at a performance anomaly.
            double dtwIncrease = (correlation.dtw - lastDtw)*100.0/lastDtw;
            if (dtwIncrease > dtwIncreaseThreshold) {
                reportAnomaly(start, end, correlation.key, String.format(
                        "Correlation: %.4f; DTW-Increase: %.4f%%", correlation.rValue, dtwIncrease));
            }
        }
        prevDtw.put(correlation.key, correlation.dtw);
    }

    private static class Correlation {

        private final String key;
        private final double rValue;
        private final double dtw;

        private Correlation(String key, double rValue, double dtw) {
            this.key = key;
            this.rValue = rValue;
            this.dtw = dtw;
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends AnomalyDetectorBuilder<CorrelationBasedDetector,Builder> {

        private double correlationThreshold = 0.5;
        private double dtwIncreaseThreshold = 20.0;

        private Builder() {
        }

        public Builder setCorrelationThreshold(double correlationThreshold) {
            this.correlationThreshold = correlationThreshold;
            return this;
        }

        public Builder setDtwIncreaseThreshold(double dtwIncreaseThreshold) {
            this.dtwIncreaseThreshold = dtwIncreaseThreshold;
            return this;
        }

        public CorrelationBasedDetector build(RootsEnvironment environment) {
            return new CorrelationBasedDetector(environment, this);
        }

        @Override
        protected Builder getThisObj() {
            return this;
        }
    }

}
