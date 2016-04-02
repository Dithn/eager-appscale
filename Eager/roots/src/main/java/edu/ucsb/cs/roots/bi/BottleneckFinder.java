package edu.ucsb.cs.roots.bi;

import com.google.common.collect.ImmutableList;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.anomaly.Anomaly;
import edu.ucsb.cs.roots.data.ApiCall;
import edu.ucsb.cs.roots.data.ApplicationRequest;
import edu.ucsb.cs.roots.data.DataStore;
import edu.ucsb.cs.roots.data.DataStoreException;
import edu.ucsb.cs.roots.rlang.RClient;
import edu.ucsb.cs.roots.rlang.RService;
import org.rosuda.REngine.REXP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class BottleneckFinder {

    private static final Logger log = LoggerFactory.getLogger(BottleneckFinder.class);

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

        RService rService = environment.getRService();
        try (RClient client = new RClient(rService)) {
            client.evalAndAssign("df", "data.frame()");
            for (ApplicationRequest r : requests) {
                double[] values = new double[callCount + 1];
                for (int i = 0; i < callCount; i++) {
                    values[i] = r.getApiCalls().get(i).getTimeElapsed();
                }
                values[callCount] = r.getResponseTime();
                client.assign("x", values);
                client.evalAndAssign("df", "rbind(df, x)");
            }

            String[] names = new String[callCount + 1];
            for (int i = 0; i < callCount; i++) {
                names[i] = String.format("X%d", i + 1);
            }
            names[callCount] = "Total";
            client.assign("df_names", names);
            client.eval("names(df) = df_names");

            client.evalAndAssign("model", "lm(Total ~ ., data=df)");
            client.evalAndAssign("rankings", "calc.relimp(model, type=c('lmg'))");
            REXP rankings = client.eval("rankings$lmg");
            double[] results = rankings.asDoubles();
            for (int i = 0; i < results.length; i++) {
                log.info("Relative importance: {}: {}", apiCalls.get(i).name(), results[i]);
            }
        } catch (Exception e) {
            log.error("Error while computing relative importance metrics", e);
        }
    }
}
