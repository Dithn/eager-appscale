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
import edu.ucsb.cs.roots.rlang.RService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public final class BottleneckFinder {

    private static final Logger log = LoggerFactory.getLogger(BottleneckFinder.class);

    private static final String LOCAL = "LOCAL";

    private final RootsEnvironment environment;

    public BottleneckFinder(RootsEnvironment environment) {
        this.environment = environment;
    }

    public void run(Anomaly anomaly) {
        long history = anomaly.getEnd() - anomaly.getStart();
        long start = anomaly.getEnd() - 2 * history;
        DataStore ds = environment.getDataStoreService().get(anomaly.getDataStore());
        try {
            ImmutableList<ApplicationRequest> requests = ds.getRequestInfo(
                    anomaly.getApplication(), anomaly.getOperation(), start, anomaly.getEnd());
            Map<String,List<ApplicationRequest>> perPathRequests = requests.stream().collect(
                    Collectors.groupingBy(ApplicationRequest::getPathAsString));
            perPathRequests.forEach((path,list) -> analyze(path, list, start,
                    anomaly.getPeriodInSeconds() * 1000));
        } catch (DataStoreException e) {
            log.error("Error while retrieving API call data", e);
        }
    }

    private void analyze(String path, List<ApplicationRequest> requests, long start, long period) {
        List<ApiCall> apiCalls = requests.get(0).getApiCalls();
        int callCount = apiCalls.size();
        if (callCount == 0) {
            return;
        } else if (requests.size() < callCount + 2) {
            log.warn("Insufficient data to perform a bottleneck identification");
            return;
        }

        long requestCount = 0;
        ListMultimap<Long,RelativeImportance> results = ArrayListMultimap.create();
        RService rService = environment.getRService();
        Map<Long,List<ApplicationRequest>> groupedByTime = requests.stream()
                .collect(Collectors.groupingBy(r -> (r.getTimestamp() - start) / period,
                        TreeMap::new, Collectors.toList()));

        RClient client = rService.borrow();
        try {
            client.evalAndAssign("df", "data.frame()");
            for (long ts : groupedByTime.keySet()) {
                for (ApplicationRequest request : groupedByTime.get(ts)) {
                    client.assign("x", getResponseTimeVector(request));
                    client.evalAndAssign("df", "rbind(df, x)");
                    if (requestCount == 0) {
                        client.assign("df_names", getColumnNames(callCount, true));
                        client.eval("names(df) = df_names");
                    }
                    requestCount++;
                }

                if (requestCount > callCount + 1) {
                    results.putAll(ts, computeRankings(client, apiCalls));
                }
            }
        } catch (Exception e) {
            log.error("Error while computing relative importance metrics", e);
        } finally {
            rService.release(client);
        }

        if (results.size() > 0) {
            List<Long> sortedKeys = results.keySet().stream().sorted().collect(Collectors.toList());
            log.info(getLogEntry(path, results.get(Iterables.getLast(sortedKeys))));
            for (int i = 0; i < callCount; i++) {
                int index = i;
                String trend = sortedKeys.stream()
                        .map(k -> String.valueOf(results.get(k).get(index).importance))
                        .collect(Collectors.joining(", "));
                log.info("{}: {}", apiCalls.get(i).name(), trend);
            }
        }
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

    /*public static void main(String[] args) throws Exception {
        RootsEnvironment environment = new RootsEnvironment("Test", new FileConfigLoader("conf"));
        environment.init();
        Runtime.getRuntime().addShutdownHook(new Thread("RootsShutdownHook") {
            @Override
            public void run() {
                environment.destroy();
            }
        });

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("PST"));
        cal.set(2016, Calendar.APRIL, 13, 15, 56, 0);
        Date start = cal.getTime();
        cal.set(2016, Calendar.APRIL, 13, 16, 2, 0);
        Date end = cal.getTime();
        CorrelationBasedDetector detector = CorrelationBasedDetector.newBuilder()
                .setApplication("javabook")
                .setPeriodInSeconds(600)
                .setDataStore("elk")
                .build(environment);
        Anomaly anomaly = new Anomaly(detector, start.getTime(), end.getTime(),
                Anomaly.TYPE_PERFORMANCE, "GET /", "foo");

        BottleneckFinder finder = new BottleneckFinder(environment);
        finder.run(anomaly);

        environment.waitFor();
    }*/
}
