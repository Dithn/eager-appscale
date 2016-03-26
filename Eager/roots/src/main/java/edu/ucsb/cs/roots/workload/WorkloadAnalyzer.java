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

import static com.google.common.base.Preconditions.checkArgument;
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

            Segment[] segments = detectChangePoints(summary);
            if (segments == null) {
                return;
            }
            for (int i = 1; i < segments.length; i++) {
                double percentageIncrease = (segments[i].mean - segments[i-1].mean)*100.0 /
                        segments[i-1].mean;
                // TODO: Find a more meaningful day to handle this information
                if (percentageIncrease > 200) {
                    log.info("Problematic workload increase at {}: {} --> {}", segments[i].start,
                            segments[i-1].mean, segments[i].mean);
                }
            }
        } catch (Exception e) {
            log.error("Error while computing workload changes for: {}", anomaly.getApplication(), e);
        }
    }

    private Segment[] detectChangePoints(List<Double> data) throws Exception {
        int[] changePoints;
        try (RClient r = new RClient(environment.getRService())) {
            double[] values = Doubles.toArray(data);
            log.info("DATA: {}", Arrays.toString(values));
            r.assign("x", values);
            r.evalAndAssign("result", "cpt.mean(x, method='PELT')");
            REXP result = r.eval("cpts(result)");
            changePoints = Arrays.stream(result.asIntegers()).map(i -> i - 1).toArray();
        }

        if (changePoints[0] == -1) {
            return null;
        }
        Segment[] segments = new Segment[changePoints.length + 1];
        int prevIndex = 0;
        for (int i = 0; i <= changePoints.length; i++) {
            int cp;
            if (i == changePoints.length) {
                cp = data.size();
            } else {
                cp = changePoints[i] + 1;
            }
            segments[i] = new Segment(prevIndex, cp, data);
            prevIndex = cp;
        }
        return segments;
    }

    private static class Segment {
        private final int start;
        private final int end;
        private final double mean;

        public Segment(int start, int end, List<Double> data) {
            checkArgument(start < end);
            log.info("Segment range: {}-{}", start, end - 1);
            this.start = start;
            this.end = end;
            this.mean = data.subList(start, end).stream().mapToDouble(Double::doubleValue)
                    .average().getAsDouble();
        }

        int length() {
            return end - start;
        }

    }

}
