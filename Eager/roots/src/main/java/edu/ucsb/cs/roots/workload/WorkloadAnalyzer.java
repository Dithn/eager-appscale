package edu.ucsb.cs.roots.workload;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.anomaly.Anomaly;
import edu.ucsb.cs.roots.anomaly.AnomalyDetector;
import edu.ucsb.cs.roots.data.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public final class WorkloadAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(WorkloadAnalyzer.class);

    private final RootsEnvironment environment;
    private final ChangePointDetector changePointDetector;

    public WorkloadAnalyzer(RootsEnvironment environment) {
        checkNotNull(environment, "Environment is required");
        this.environment = environment;
        this.changePointDetector = new PELTChangePointDetector(environment);
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

            Segment[] segments = changePointDetector.computeSegments(summary);
            if (segments == null) {
                return;
            }
            for (int i = 1; i < segments.length; i++) {
                double percentageIncrease = segments[i-1].percentageIncrease(segments[i]);
                // TODO: Find a more meaningful day to handle this information
                if (percentageIncrease > 200) {
                    log.info("Problematic workload increase at {}: {} --> {}", segments[i].getStart(),
                            segments[i-1].getMean(), segments[i].getMean());
                }
            }
        } catch (Exception e) {
            log.error("Error while computing workload changes for: {}", anomaly.getApplication(), e);
        }
    }
}
