package edu.ucsb.cs.roots.anomaly;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.data.ApplicationRequest;
import edu.ucsb.cs.roots.data.DataStore;
import edu.ucsb.cs.roots.data.DataStoreException;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.*;
import java.util.stream.Collectors;

public final class PathAnomalyDetector extends AnomalyDetector {

    private final Map<String,ListMultimap<String,PathRatio>> history = new HashMap<>();
    private long end = -1L;

    private PathAnomalyDetector(RootsEnvironment environment, Builder builder) {
        super(environment, builder.application, builder.periodInSeconds,
                builder.historyLengthInSeconds, builder.dataStore, builder.properties);
    }

    @Override
    void run(long now) {
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
            history.forEach(this::analyzePathDistributions);
        } catch (DataStoreException e) {
            String msg = "Error while retrieving data";
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    private void analyzePathDistributions(String op, ListMultimap<String,PathRatio> pathData) {
        pathData.keySet().stream().forEach(path -> {
            SummaryStatistics statistics = new SummaryStatistics();
            List<PathRatio> pathRatios = pathData.get(path);
            pathRatios.forEach(v -> statistics.addValue(v.ratio));
            log.info("{} {} - Mean: {}, Std.Dev: {}, Count: {}", op, path, statistics.getMean(),
                    statistics.getStandardDeviation(), pathRatios.size());
        });
    }

    private void updateHistory(long windowStart, long windowEnd) throws DataStoreException {
        DataStore ds = environment.getDataStoreService().get(this.dataStore);
        Map<String,ImmutableList<ApplicationRequest>> requests = ds.getRequestInfo(
                application, windowStart, windowEnd);
        requests.forEach((k,v) -> updateOperationHistory(k, v, windowStart));

        history.keySet().stream().filter(op -> !requests.containsKey(op)).forEach(op -> {
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
            opHistory.put(path, new PathRatio(windowStart, count, totalRequests));
        });
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

        private Builder() {
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
