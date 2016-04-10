package edu.ucsb.cs.roots.anomaly;

import com.google.common.base.Strings;
import com.google.common.collect.*;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.data.DataStore;
import edu.ucsb.cs.roots.data.DataStoreException;
import edu.ucsb.cs.roots.data.ResponseTimeSummary;
import edu.ucsb.cs.roots.rlang.RClient;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.rosuda.REngine.REXP;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;

public final class CorrelationBasedDetector extends AnomalyDetector {

    /**
     * Detect DTW increase by comparing the new value to the last computed DTW
     * distance, and calculating the percentage change.
     */
    private static final String DTW_ANALYSIS_COMPARE_TO_LAST = "compare.to.last";

    /**
     * Detect DTW increase by comparing the new value to the mean of the all previously
     * calculated DTW distances.
     */
    private static final String DTW_ANALYSIS_COMPARE_TO_ALL = "compare.to.all";

    private static final ImmutableSet<String> DTW_ANALYSIS =
            ImmutableSet.of(DTW_ANALYSIS_COMPARE_TO_LAST, DTW_ANALYSIS_COMPARE_TO_ALL);

    private final ListMultimap<String,ResponseTimeSummary> history;
    private final ListMultimap<String,DTWDistance> dtwTrends;
    private final double correlationThreshold;
    private final String dtwAnalysis;
    private final double dtwMeanThreshold;
    private final double dtwIncreaseThreshold;

    private long end = -1L;

    private CorrelationBasedDetector(RootsEnvironment environment, Builder builder) {
        super(environment, builder.application, builder.periodInSeconds,
                builder.historyLengthInSeconds, builder.dataStore, builder.properties);
        checkArgument(builder.correlationThreshold >= -1 && builder.correlationThreshold <= 1,
                "Correlation threshold must be in the interval [-1,1]");
        checkArgument(!Strings.isNullOrEmpty(builder.dtwAnalysis), "DTW analysis method is required");
        checkArgument(DTW_ANALYSIS.contains(builder.dtwAnalysis),
                "Unsupported DTW analysis method: %s", builder.dtwAnalysis);
        checkArgument(builder.dtwMeanThreshold > 0, "DTW mean threshold must be positive");
        checkArgument(builder.dtwIncreaseThreshold > 0, "DTW increase threshold must be positive");
        this.history = ArrayListMultimap.create();
        this.dtwTrends = ArrayListMultimap.create();
        this.correlationThreshold = builder.correlationThreshold;
        this.dtwAnalysis = builder.dtwAnalysis;
        this.dtwMeanThreshold = builder.dtwMeanThreshold;
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
        dtwTrends.values().removeIf(d -> d.timestamp < cutoff);
        history.keySet().stream()
                .filter(k -> requestTypes.contains(k) && history.get(k).size() > 2)
                .map(k -> computeCorrelation(k, history.get(k)))
                .filter(Objects::nonNull)
                .forEach(c -> checkForAnomalies(cutoff, end, c));
    }

    private void initFullHistory(long windowStart, long windowEnd) throws DataStoreException {
        checkArgument(windowStart < windowEnd, "Start time must precede end time");
        if (log.isDebugEnabled()) {
            log.debug("Initializing history for {} ({} - {})", application, new Date(windowStart),
                    new Date(windowEnd));
        }
        DataStore ds = environment.getDataStoreService().get(this.dataStore);
        ImmutableListMultimap<String,ResponseTimeSummary> summaries =
                ds.getResponseTimeHistory(application, windowStart, windowEnd,
                        periodInSeconds * 1000);
        history.putAll(summaries);
        history.keySet().stream()
                .filter(op -> history.get(op).size() > 2)
                .forEach(op -> dtwTrends.putAll(op, computeDTWTrend(history.get(op))));
    }

    private Collection<String> updateHistory(long windowStart,
                                             long windowEnd) throws DataStoreException {
        checkArgument(windowStart < windowEnd, "Start time must precede end time");
        if (log.isDebugEnabled()) {
            log.debug("Updating history for {} ({} - {})", application, new Date(windowStart),
                    new Date(windowEnd));
        }
        DataStore ds = environment.getDataStoreService().get(this.dataStore);
        ImmutableMap<String,ResponseTimeSummary> summaries = ds.getResponseTimeSummary(
                application, windowStart, windowEnd);
        summaries.forEach(history::put);
        return ImmutableList.copyOf(summaries.keySet());
    }

    private Correlation computeCorrelation(String operation, List<ResponseTimeSummary> summaries) {
        double[] requests = new double[summaries.size()];
        double[] responseTime = new double[summaries.size()];
        for (int i = 0; i < summaries.size(); i++) {
            ResponseTimeSummary s = summaries.get(i);
            requests[i] = s.getRequestCount();
            responseTime[i] = s.getMeanResponseTime();
        }

        if (log.isDebugEnabled()) {
            log.debug("Request Counts: {}", Arrays.toString(requests));
            log.debug("Response Times: {}", Arrays.toString(responseTime));
        }

        try (RClient r = new RClient(environment.getRService())) {
            r.assign("x", requests);
            r.assign("y", responseTime);
            REXP correlation = r.eval("cor(x, y, method='pearson')");
            r.evalAndAssign("time_warp", "dtw(x, y)");
            REXP distance = r.eval("time_warp$distance");
            log.info("Correlation analysis output [{}]: {} {} {}", operation, correlation.asDouble(),
                    distance.asDouble(), requests.length);
            return new Correlation(operation, correlation.asDouble(), distance.asDouble());
        } catch (Exception e) {
            log.error("Error computing the correlation statistics", e);
            return null;
        }
    }

    private List<DTWDistance> computeDTWTrend(List<ResponseTimeSummary> summaries) {
        List<DTWDistance> trend = new ArrayList<>();
        try (RClient r = new RClient(environment.getRService())) {
            r.evalAndAssign("x", "c()");
            r.evalAndAssign("y", "c()");
            int count = 0;
            for (ResponseTimeSummary summary : summaries) {
                r.evalAndAssign("x", String.format("c(x,%f)", summary.getRequestCount()));
                r.evalAndAssign("y", String.format("c(y,%f)", summary.getMeanResponseTime()));
                if (++count > 2) {
                    r.evalAndAssign("time_warp", "dtw(x, y)");
                    REXP distance = r.eval("time_warp$distance");
                    double dtw = distance.asDouble();
                    trend.add(new DTWDistance(summary.getTimestamp(), dtw));

                    SummaryStatistics statistics = new SummaryStatistics();
                    trend.forEach(d -> statistics.addValue(d.dtw));
                    cleanUpDTWTrend(trend, statistics, summary.getTimestamp());
                }
            }
        } catch (Exception e) {
            log.error("Error computing the DTW trend", e);
        }
        return ImmutableList.copyOf(trend);
    }

    private void checkForAnomalies(long start, long end, Correlation correlation) {
        final long currentTimestamp = end - periodInSeconds * 1000;
        dtwTrends.put(correlation.operation, new DTWDistance(currentTimestamp, correlation.dtw));

        List<DTWDistance> trend = dtwTrends.get(correlation.operation);
        SummaryStatistics statistics = new SummaryStatistics();
        trend.forEach(d -> statistics.addValue(d.dtw));

        boolean dtwIncreased = false;
        if (DTW_ANALYSIS_COMPARE_TO_LAST.equals(dtwAnalysis)) {
            if (trend.size() > 2) {
                double penultimate = trend.get(trend.size() - 2).dtw;
                double increase = (correlation.dtw - penultimate) * 100.0 / penultimate;
                log.debug("DTW increase from the last value: {}%", increase);
                dtwIncreased = increase > dtwIncreaseThreshold;
            }
        } else {
            double upper = statistics.getMean() + dtwMeanThreshold * statistics.getStandardDeviation();
            log.debug("DTW threshold: {}, Current: {}", upper, correlation.dtw);
            dtwIncreased = correlation.dtw > upper;
            cleanUpDTWTrend(trend, statistics, currentTimestamp);
        }

        if (correlation.rValue < correlationThreshold && dtwIncreased) {
            // If the correlation has dropped and the DTW distance has increased, we
            // might be looking at a performance anomaly.
            double dtwIncrease = (correlation.dtw - statistics.getMean())*100.0/statistics.getMean();
            reportAnomaly(start, end, correlation.operation, String.format(
                    "Correlation: %.4f; DTW-Increase: %.4f%%", correlation.rValue, dtwIncrease));
        }
    }

    private void cleanUpDTWTrend(
            List<DTWDistance> trend, SummaryStatistics statistics, long timestamp) {
        final double upper = statistics.getMean() +
                dtwMeanThreshold * statistics.getStandardDeviation();
        final double lower = statistics.getMean() -
                dtwMeanThreshold * statistics.getStandardDeviation();
        final double dtw = Iterables.getLast(trend).dtw;
        if (dtw > upper || dtw < lower) {
            log.debug("Cleaning up DTW history up to {}", timestamp);
            trend.removeIf(d -> d.timestamp < timestamp);
        }
    }

    private static class Correlation {

        private final String operation;
        private final double rValue;
        private final double dtw;

        private Correlation(String operation, double rValue, double dtw) {
            this.operation = operation;
            this.rValue = rValue;
            this.dtw = dtw;
        }
    }

    private static class DTWDistance {

        private final long timestamp;
        private final double dtw;

        private DTWDistance(long timestamp, double dtw) {
            this.timestamp = timestamp;
            this.dtw = dtw;
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends AnomalyDetectorBuilder<CorrelationBasedDetector,Builder> {

        private double correlationThreshold = 0.5;
        private String dtwAnalysis = DTW_ANALYSIS_COMPARE_TO_ALL;
        private double dtwMeanThreshold = 2.0;
        private double dtwIncreaseThreshold = 200.0;

        private Builder() {
        }

        public Builder setCorrelationThreshold(double correlationThreshold) {
            this.correlationThreshold = correlationThreshold;
            return this;
        }

        public Builder setDtwAnalysis(String dtwAnalysis) {
            this.dtwAnalysis = dtwAnalysis;
            return this;
        }

        public Builder setDtwMeanThreshold(double dtwMeanThreshold) {
            this.dtwMeanThreshold = dtwMeanThreshold;
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
