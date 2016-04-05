package edu.ucsb.cs.roots.bi;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Doubles;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.anomaly.Anomaly;
import edu.ucsb.cs.roots.data.ApiCall;
import edu.ucsb.cs.roots.data.ApplicationRequest;
import edu.ucsb.cs.roots.data.DataStore;
import edu.ucsb.cs.roots.data.DataStoreException;
import edu.ucsb.cs.roots.rlang.RClient;
import edu.ucsb.cs.roots.rlang.RService;
import edu.ucsb.cs.roots.workload.CLChangePointDetector;
import edu.ucsb.cs.roots.workload.Segment;
import org.rosuda.REngine.REXP;
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
            perPathRequests.forEach(this::analyze);
        } catch (DataStoreException e) {
            log.error("Error while retrieving API call data", e);
        }
    }

    private void analyze(String path, List<ApplicationRequest> requests) {
        List<ApiCall> apiCalls = requests.get(0).getApiCalls();
        int callCount = apiCalls.size();
        if (callCount == 0) {
            return;
        } else if (requests.size() < callCount + 2) {
            log.warn("Insufficient data to perform a bottleneck identification");
            return;
        }

        Map<ApiCall,double[]> importanceTrend = new HashMap<>();
        RService rService = environment.getRService();
        try (RClient client = new RClient(rService)) {
            client.evalAndAssign("df", "data.frame()");
            client.evalAndAssign("df_r", "data.frame()");
            for (int i = 0; i < requests.size(); i++) {
                client.assign("x", getResponseTimeVector(requests.get(i)));
                client.evalAndAssign("df", "rbind(df, x)");
                if (i == 0) {
                    client.assign("df_names", getColumnNames(callCount, true));
                    client.eval("names(df) = df_names");
                } else if (i > callCount) {
                    client.evalAndAssign("model", "lm(Total ~ ., data=df)");
                    client.evalAndAssign("rankings", "calc.relimp(model, type=c('lmg'))");
                    client.evalAndAssign("df_r", "rbind(df_r, rankings$lmg)");
                    if (i == callCount + 1) {
                        client.assign("df_names", getColumnNames(callCount, false));
                        client.eval("names(df_r) = df_names");
                    }
                }
            }

            for (int i = 0; i < apiCalls.size(); i++) {
                REXP col = client.eval(String.format("df_r[,%d]", i + 1));
                importanceTrend.put(apiCalls.get(i), col.asDoubles());
            }

            REXP rankings = client.eval("rankings$lmg");

            List<RelativeImportance> result = getRelativeImportance(apiCalls, rankings.asDoubles());
            log.info(getLogEntry(path, result));
        } catch (Exception e) {
            log.error("Error while computing relative importance metrics", e);
        }

        apiCalls.forEach(call -> analyzeTrend(rService, call.name(), importanceTrend.get(call)));
    }

    private void analyzeTrend(RService rService, String call, double[] trend) {
        try {
            CLChangePointDetector cpd = new CLChangePointDetector(rService);
            log.info("**** {}", Arrays.toString(trend));
            Segment[] segments = cpd.computeSegments(trend);
            if (segments.length == 1) {
                log.info("{}: No significant changes in trend", call);
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append(segments[0].getMean());
                for (int i = 1; i < segments.length; i++) {
                    sb.append(" -> ").append(segments[i].getMean());
                }
                log.info("{}: {}", call, sb.toString());
            }
        } catch (Exception e) {
            log.error("Error while analyzing relative importance trend", e);
        }
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

    private List<RelativeImportance> getRelativeImportance(List<ApiCall> apiCalls, double[] rankings) {
        List<RelativeImportance> result = new ArrayList<>(rankings.length);
        for (int i = 0; i < rankings.length; i++) {
            result.add(new RelativeImportance(apiCalls.get(i).name(), rankings[i]));
        }
        result.add(new RelativeImportance(LOCAL, 1.0 - result.stream()
                .mapToDouble(r -> r.importance).sum()));

        // Set rankings based on the importance score
        Set<RelativeImportance> indexSet = new TreeSet<>(Collections.reverseOrder());
        indexSet.addAll(result);
        int rank = 1;
        for (RelativeImportance ri : indexSet) {
            ri.ranking = rank++;
        }
        return result;
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

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2016, Calendar.JANUARY, 16, 0, 0, 0);
        Date start = cal.getTime();
        cal.set(2016, Calendar.JANUARY, 16, 1, 0, 0);
        Date end = cal.getTime();
        CorrelationBasedDetector detector = CorrelationBasedDetector.newBuilder()
                .setApplication("watchtower")
                .build(environment);
        Anomaly anomaly = new Anomaly(detector, start.getTime(), end.getTime(), "GET /foo", "foo");

        BottleneckFinder finder = new BottleneckFinder(environment);
        finder.run(anomaly);

        environment.waitFor();
    }*/
}
