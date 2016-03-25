package edu.ucsb.cs.roots.workload;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.anomaly.Anomaly;
import edu.ucsb.cs.roots.anomaly.AnomalyDetector;
import edu.ucsb.cs.roots.data.DataStore;
import edu.ucsb.cs.roots.data.DataStoreException;
import edu.ucsb.cs.roots.rlang.RClient;
import org.rosuda.REngine.REXP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

public class WorkloadChangeDetector {

    private static final Logger log = LoggerFactory.getLogger(WorkloadChangeDetector.class);

    private final RootsEnvironment environment;
    private final Cache<String,Long> changePointCache = CacheBuilder.newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .build();

    public WorkloadChangeDetector(RootsEnvironment environment) {
        checkNotNull(environment, "Environment is required");
        this.environment = environment;
    }

    @Subscribe
    public void analyzeWorkload(Anomaly anomaly) {
        String key = anomaly.getApplication() + " " + anomaly.getOperation();
        Long lastChangePoint = changePointCache.getIfPresent(key);
        long start;
        if (lastChangePoint != null) {
            start = lastChangePoint + 1;
        } else {
            long history = anomaly.getEnd() - anomaly.getStart();
            start = anomaly.getEnd() - 2 * history;
        }

        AnomalyDetector detector = environment.getAnomalyDetectorService()
                .getDetector(anomaly.getApplication());
        DataStore dataStore = environment.getDataStoreService().get(detector.getDataStore());
        try {
            ImmutableList<Double> summary = dataStore.getWorkloadSummary(anomaly.getApplication(),
                    anomaly.getOperation(), start, anomaly.getEnd(),
                    detector.getPeriodInSeconds() * 1000);
            if (summary.size() > 0) {
                double[] data = new double[summary.size()];
                for (int i = 0; i < summary.size(); i++) {
                    data[i] = summary.get(i);
                }
                detectChangePoints(data);
            }
        } catch (DataStoreException e) {
            log.error("Error while loading workload history", e);
        }
    }

    private void detectChangePoints(double[] data) {
        try (RClient r = new RClient(environment.getRService())) {
            r.assign("x", data);
            r.evalAndAssign("result", "cpt.mean(x, method='PELT')");
            REXP changePoints = r.eval("cpts(result)");
            int[] index = changePoints.asIntegers();
            log.info("Input data: {}", Arrays.toString(data));
            for (int i : index) {
                log.info("Change Point index {}: {}", i, data[i]);
            }
        } catch (Exception e) {
            log.error("Error computing the change points", e);
        }
    }

}
