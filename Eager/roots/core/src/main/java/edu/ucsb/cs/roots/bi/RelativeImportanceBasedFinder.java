package edu.ucsb.cs.roots.bi;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.primitives.Doubles;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.anomaly.Anomaly;
import edu.ucsb.cs.roots.data.ApiCall;
import edu.ucsb.cs.roots.data.ApplicationRequest;
import edu.ucsb.cs.roots.data.DataStore;
import edu.ucsb.cs.roots.data.DataStoreException;
import edu.ucsb.cs.roots.rlang.RClient;

import java.util.*;
import java.util.stream.Collectors;

public final class RelativeImportanceBasedFinder extends BottleneckFinder {

    public RelativeImportanceBasedFinder(RootsEnvironment environment) {
        super(environment);
    }

    @Override
    void analyze(Anomaly anomaly) {
        long history = anomaly.getEnd() - anomaly.getStart();
        long start = anomaly.getEnd() - 2 * history;
        DataStore ds = environment.getDataStoreService().get(anomaly.getDataStore());
        try {
            ImmutableList<ApplicationRequest> requests = ds.getRequestInfo(
                    anomaly.getApplication(), anomaly.getOperation(), start, anomaly.getEnd());
            log.debug("Received {} requests for analysis", requests.size());
            Map<String,List<ApplicationRequest>> perPathRequests = requests.stream().collect(
                    Collectors.groupingBy(ApplicationRequest::getPathAsString));
            perPathRequests.forEach((path,list) -> analyze(anomaly, path, list, start));
        } catch (DataStoreException e) {
            anomalyLog.error(anomaly, "Error while retrieving API call data", e);
        }
    }

    private void analyze(Anomaly anomaly, String path, List<ApplicationRequest> requests,
                         long start) {
        List<ApiCall> apiCalls = requests.get(0).getApiCalls();
        int callCount = apiCalls.size();
        if (callCount == 0) {
            return;
        } else if (requests.size() < callCount + 2) {
            anomalyLog.warn(anomaly, "Insufficient data to perform a bottleneck identification");
            return;
        }

        long period = anomaly.getPeriodInSeconds() * 1000;
        Map<Long,List<ApplicationRequest>> groupedByTime = requests.stream()
                .collect(Collectors.groupingBy(r -> groupByTime(r, start, period),
                        TreeMap::new, Collectors.toList()));
        try {
            ListMultimap<Long,RelativeImportance> results = computeRankings(apiCalls, groupedByTime);
            if (results.isEmpty()) {
                return;
            }
            List<Long> sortedKeys = results.keySet().stream()
                    .sorted().collect(Collectors.toList());
            anomalyLog.info(anomaly, getLogEntry(path, results.get(Iterables.getLast(sortedKeys))));
            for (int i = 0; i < callCount; i++) {
                int index = i;
                String trend = sortedKeys.stream()
                        .map(k -> String.valueOf(results.get(k).get(index).importance))
                        .collect(Collectors.joining(", "));
                anomalyLog.info(anomaly, "Historical trend for {}: {}",
                        apiCalls.get(i).name(), trend);
            }
        } catch (Exception e) {
            anomalyLog.error(anomaly, "Error while computing rankings", e);
        }
    }

    private long groupByTime(ApplicationRequest r, long start, long period) {
        long index = (r.getTimestamp() - start) / period;
        return index * period + start;
    }

    private ListMultimap<Long,RelativeImportance> computeRankings(
            List<ApiCall> apiCalls, Map<Long, List<ApplicationRequest>> groupedByTime) throws Exception {
        long requestCount = 0;
        ListMultimap<Long,RelativeImportance> results = ArrayListMultimap.create();
        List<Exception> rankingErrors = new ArrayList<>();
        RClient client = environment.getRService().borrow();
        try {
            client.evalAndAssign("df", "data.frame()");
            for (long ts : groupedByTime.keySet()) {
                for (ApplicationRequest request : groupedByTime.get(ts)) {
                    double[] responseTimeVector = getResponseTimeVector(request);
                    if (log.isDebugEnabled()) {
                        log.debug("Response time vector: {}", Arrays.toString(responseTimeVector));
                    }
                    client.assign("x", responseTimeVector);
                    client.evalAndAssign("df", "rbind(df, x)");
                    if (requestCount == 0) {
                        client.assign("df_names", getColumnNames(apiCalls.size(), true));
                        client.eval("names(df) = df_names");
                    }
                    requestCount++;
                }

                if (requestCount > apiCalls.size() + 1) {
                    try {
                        results.putAll(ts, computeRankings(client, apiCalls));
                    } catch (Exception e) {
                        rankingErrors.add(e);
                    }
                }
            }
        } finally {
            environment.getRService().release(client);
        }

        if (results.isEmpty() && !rankingErrors.isEmpty()) {
            throw new RuntimeException(rankingErrors.size() + " errors encountered",
                    Iterables.getLast(rankingErrors));
        }
        return results;
    }

    /**
     * Returns a List of RelativeImportance objects (one object per ApiCall). The returned list's
     * order corresponds to the order of the input ApiCall list. The rankings attribute on each
     * RelativeImportance instance is set according to the decreasing order of the relative
     * importance metric.
     */
    private List<RelativeImportance> computeRankings(RClient client,
                                                     List<ApiCall> apiCalls) throws Exception {
        client.evalAndAssign("model", "lm(Total ~ ., data=df)");
        client.evalAndAssign("rankings", "calc.relimp(model, type=c('lmg'))");
        double[] rankings = client.evalToDoubles("rankings$lmg");
        List<RelativeImportance> result = new ArrayList<>(rankings.length);
        for (int i = 0; i < rankings.length; i++) {
            result.add(new RelativeImportance(apiCalls.get(i).name(), rankings[i]));
        }
        result.add(new RelativeImportance(LOCAL, 1.0 - result.stream()
                .mapToDouble(r -> r.importance).sum()));

        // Set rankings based on the importance score
        List<RelativeImportance> sorted = result.stream().sorted(Collections.reverseOrder())
                .collect(Collectors.toList());
        int rank = 1;
        for (RelativeImportance ri : sorted) {
            ri.ranking = rank++;
        }
        return result;
    }

    private String[] getColumnNames(int callCount, boolean total) {
        String[] names;
        if (total) {
            names = new String[callCount + 1];
            names[callCount] = "Total";
        } else {
            names = new String[callCount];
        }
        for (int i = 0; i < callCount; i++) {
            names[i] = String.format("X%d", i + 1);
        }
        return names;
    }

    private double[] getResponseTimeVector(ApplicationRequest request) {
        List<Integer> vector = request.getApiCalls().stream().map(ApiCall::getTimeElapsed)
                .collect(Collectors.toCollection(ArrayList::new));
        vector.add(request.getResponseTime());
        return Doubles.toArray(vector);
    }

    private String getLogEntry(String path, List<RelativeImportance> result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Relative importance metrics for path: ").append(path).append('\n');
        result.forEach(r -> sb.append(r).append('\n'));
        sb.append('\n');
        sb.append("Total variance explained: ").append(result.stream()
                .filter(r -> !r.apiCall.equals(LOCAL))
                .mapToDouble(r -> r.importance).sum());
        return sb.toString();
    }

    private static class RelativeImportance implements Comparable<RelativeImportance> {
        private final String apiCall;
        private final double importance;
        private int ranking;

        RelativeImportance(String apiCall, double importance) {
            this.apiCall = apiCall;
            this.importance = importance;
        }

        @Override
        public int compareTo(RelativeImportance o) {
            return Double.compare(this.importance, o.importance);
        }

        @Override
        public String toString() {
            return String.format("[%2d] %s %f", ranking, apiCall, importance);
        }
    }
}