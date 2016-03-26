package edu.ucsb.cs.roots.workload;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.Doubles;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.anomaly.Anomaly;
import edu.ucsb.cs.roots.anomaly.AnomalyDetector;
import edu.ucsb.cs.roots.data.DataStore;
import edu.ucsb.cs.roots.rlang.RClient;
import org.rosuda.REngine.REXP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public final class WorkloadAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(WorkloadAnalyzer.class);

    private final RootsEnvironment environment;

    public WorkloadAnalyzer(RootsEnvironment environment) {
        checkNotNull(environment, "Environment is required");
        this.environment = environment;
    }

    @Subscribe
    public void analyzeWorkload(Anomaly anomaly) {
        long history = anomaly.getEnd() - anomaly.getStart();
        long start = anomaly.getEnd() - 2 * history;

        AnomalyDetector detector = environment.getAnomalyDetectorService()
                .getDetector(anomaly.getApplication());
        DataStore dataStore = environment.getDataStoreService().get(detector.getDataStore());
        try {
            ImmutableList<Double> summary = dataStore.getWorkloadSummary(anomaly.getApplication(),
                    anomaly.getOperation(), start, anomaly.getEnd(),
                    detector.getPeriodInSeconds() * 1000);
            if (summary.size() == 0) {
                log.warn("No workload data found for {}", anomaly.getApplication());
                return;
            }

            int[] changePoints = detectChangePoints(summary);
            if (changePoints[0] == -1) {
                return;
            }

            double[] segments = computeSegments(changePoints, summary);
            for (int i = 1; i < segments.length; i++) {
                double percentageIncrease = (segments[i] - segments[i-1])*100.0 / segments[i-1];
                if (percentageIncrease > 50) {
                    log.info("Problematic workload increase at {}: {} --> {}", changePoints[i-1],
                            segments[i-1], segments[i]);
                } else {
                    log.info("Workload change at {}: {} --> {}", changePoints[i-1],
                            segments[i-1], segments[i]);
                }
            }
        } catch (Exception e) {
            log.error("Error while computing workload changes for: {}", anomaly.getApplication(), e);
        }
    }

    private int[] detectChangePoints(List<Double> data) throws Exception {
        try (RClient r = new RClient(environment.getRService())) {
            r.assign("x", Doubles.toArray(data));
            r.evalAndAssign("result", "cpt.mean(x, method='PELT')");
            REXP changePoints = r.eval("cpts(result)");
            return Arrays.stream(changePoints.asIntegers()).map(i -> i - 1).toArray();
        }
    }

    private double[] computeSegments(int[] changePoints, List<Double> data) {
        double[] segments = new double[changePoints.length + 1];
        int prevIndex = 0;
        for (int i = 0; i <= changePoints.length; i++) {
            int cp;
            if (i == changePoints.length) {
                cp = data.size();
            } else {
                cp = changePoints[i] + 1;
            }
            segments[i] = data.subList(prevIndex, cp).stream().mapToDouble(Double::doubleValue)
                    .average().getAsDouble();
            prevIndex = cp;
        }
        return segments;
    }

}
