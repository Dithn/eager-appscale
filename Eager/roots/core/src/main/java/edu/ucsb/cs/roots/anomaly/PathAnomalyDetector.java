package edu.ucsb.cs.roots.anomaly;

import com.google.common.collect.*;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.data.ApplicationRequest;
import edu.ucsb.cs.roots.data.DataStore;
import edu.ucsb.cs.roots.data.DataStoreException;
import edu.ucsb.cs.roots.utils.StatSummary;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

public final class PathAnomalyDetector extends AnomalyDetector {

    private final Map<String,ListMultimap<String,PathRatio>> history = new HashMap<>();
    private final double meanThreshold;

    private long end = -1L;

    private PathAnomalyDetector(RootsEnvironment environment, Builder builder) {
        super(environment, builder.application, builder.periodInSeconds,
                builder.historyLengthInSeconds, builder.dataStore, builder.properties);
        checkArgument(builder.meanThreshold > 0, "Mean threshold must be positive");
        this.meanThreshold = builder.meanThreshold;
    }

    @Override
    public void run(long now) {
        long tempStart, tempEnd;
        if (end < 0) {
            tempEnd = now - 60 * 1000;
            tempStart = tempEnd - historyLengthInSeconds * 1000;
            // TODO: Get historical path distribution records
            end = tempEnd;
        }
        tempStart = end;
        tempEnd = end + periodInSeconds * 1000;

        try {
            updateHistory(tempStart, tempEnd);
            end = tempEnd;

            long cutoff = end - historyLengthInSeconds * 1000;
            history.values().forEach(opHistory -> opHistory.values().removeIf(
                    s -> s.timestamp < cutoff));
            history.forEach((op,data) -> analyzePathDistributions(cutoff, end, op, data));
        } catch (DataStoreException e) {
            String msg = "Error while retrieving data";
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    private void analyzePathDistributions(long start, long end, String op,
                                          ListMultimap<String,PathRatio> pathData) {
        pathData.keySet().stream().forEach(path -> {
            List<PathRatio> pathRatios = pathData.get(path);
            StatSummary statistics = StatSummary.calculate(pathRatios.stream()
                    .mapToDouble(v -> v.ratio));
            log.info("[{}: {}] {} - Mean: {}, Std.Dev: {}, Count: {}", application, op, path,
                    statistics.getMean(), statistics.getStandardDeviation(), pathRatios.size());
            PathRatio last = Iterables.getLast(pathRatios);
            if (statistics.isAnomaly(last.ratio, meanThreshold)) {
                String desc = String.format("Path distribution change for: %s [%f%%]", path,
                        statistics.percentageDifference(last.ratio));
                pathRatios.removeIf(v -> v.timestamp < last.timestamp);
                reportAnomaly(start, end, Anomaly.TYPE_WORKLOAD, op, desc);
            }
        });
    }

    private void updateHistory(long windowStart, long windowEnd) throws DataStoreException {
        if (log.isDebugEnabled()) {
            log.debug("Updating history for {} ({} - {})", application, new Date(windowStart),
                    new Date(windowEnd));
        }
        DataStore ds = environment.getDataStoreService().get(this.dataStore);
        ImmutableListMultimap<String,ApplicationRequest> requests = ds.getRequestInfo(
                application, windowStart, windowEnd);
        requests.keySet().forEach(k -> updateOperationHistory(k, requests.get(k), windowStart));

        history.keySet().stream().filter(op -> !requests.containsKey(op)).forEach(op -> {
            // Inject 0's for operations not invoked in this window
            ListMultimap<String, PathRatio> pathData = history.get(op);
            pathData.keySet().forEach(path -> pathData.put(path, PathRatio.zero(windowStart)));
        });
    }

    private void updateOperationHistory(String op, ImmutableList<ApplicationRequest> perOpRequests,
                                        long windowStart) {
        ListMultimap<String,PathRatio> opHistory;
        if (history.containsKey(op)) {
            opHistory = history.get(op);
        } else {
            opHistory = ArrayListMultimap.create();
            history.put(op, opHistory);
        }

        List<PathRatio> longestPathHistory = opHistory.keySet().stream().reduce((k1, k2) -> {
            if (opHistory.get(k1).size() > opHistory.get(k2).size()) {
                return k1;
            }
            return k2;
        }).map(opHistory::get).orElse(ImmutableList.of());

        Map<String,Integer> pathRequests = perOpRequests.stream()
                .collect(Collectors.groupingBy(ApplicationRequest::getPathAsString))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size()));
        long totalRequests = pathRequests.values().stream()
                .mapToInt(Integer::intValue).sum();
        pathRequests.forEach((path,count) -> {
            if (!opHistory.containsKey(path)) {
                log.info("New path detected. Application: {}; Operation: {}, Path: {}",
                        application, op, path);
                longestPathHistory.forEach(p -> opHistory.put(path, PathRatio.zero(p.timestamp)));
            }
            PathRatio pr = new PathRatio(windowStart, count, totalRequests);
            log.debug("Path ratio update. {}: {}", path, pr.ratio);
            opHistory.put(path, pr);
        });
        // Inject 0's for paths not invoked in this window
        opHistory.keySet().stream()
                .filter(k -> !pathRequests.containsKey(k))
                .forEach(k -> opHistory.put(k, PathRatio.zero(windowStart)));
    }

    private final static class PathRatio {
        private final long timestamp;
        private final double ratio;

        PathRatio(long timestamp, long count, long total) {
            this.timestamp = timestamp;
            this.ratio = (count * 100.0)/total;
        }

        static PathRatio zero(long timestamp) {
            return new PathRatio(timestamp, 0, 1);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends AnomalyDetectorBuilder<PathAnomalyDetector,Builder> {

        private double meanThreshold = 2.0;

        private Builder() {
        }

        public Builder setMeanThreshold(double meanThreshold) {
            this.meanThreshold = meanThreshold;
            return this;
        }

        @Override
        public PathAnomalyDetector build(RootsEnvironment environment) {
            return new PathAnomalyDetector(environment, this);
        }

        @Override
        protected Builder getThisObj() {
            return this;
        }
    }
}
