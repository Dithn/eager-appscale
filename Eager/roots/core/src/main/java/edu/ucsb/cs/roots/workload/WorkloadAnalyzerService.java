package edu.ucsb.cs.roots.workload;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.Doubles;
import edu.ucsb.cs.roots.ManagedService;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.anomaly.Anomaly;
import edu.ucsb.cs.roots.anomaly.AnomalyLog;
import edu.ucsb.cs.roots.data.DataStore;

public final class WorkloadAnalyzerService extends ManagedService {

    private static final String WORKLOAD_ANALYZER = "workload.analyzer";
    private static final String DEFAULT_CHANGE_POINT_DETECTOR = "PELT";

    private final AnomalyLog anomalyLog;

    public WorkloadAnalyzerService(RootsEnvironment environment) {
        super(environment);
        this.anomalyLog = new AnomalyLog(log);
    }

    @Override
    protected void doInit() throws Exception {
        environment.subscribe(this);
    }

    @Subscribe
    public void analyzeWorkload(Anomaly anomaly) {
        long history = anomaly.getEnd() - anomaly.getStart();
        long start = anomaly.getEnd() - 2 * history;

        DataStore dataStore = environment.getDataStoreService().get(anomaly.getDataStore());
        ChangePointDetector changePointDetector = getChangePointDetector(anomaly);
        try {
            ImmutableList<Double> summary = dataStore.getWorkloadSummary(anomaly.getApplication(),
                    anomaly.getOperation(), start, anomaly.getEnd(),
                    anomaly.getPeriodInSeconds() * 1000);
            if (summary.size() == 0) {
                anomalyLog.warn(anomaly, "No workload data found");
                return;
            }

            Segment[] segments = changePointDetector.computeSegments(Doubles.toArray(summary));
            analyzeSegments(segments, anomaly);
        } catch (Exception e) {
            anomalyLog.error(anomaly, "Error while computing workload changes", e);
        }
    }

    private void analyzeSegments(Segment[] segments, Anomaly anomaly) {
        int length = segments.length;
        if (length == 1) {
            anomalyLog.info(anomaly, "No significant changes in workload to report");
            return;
        }

        for (int i = 1; i < segments.length; i++) {
            anomalyLog.info(anomaly, "Workload level shift at {}: {} --> {}", segments[i].getStart(),
                    segments[i-1].getMean(), segments[i].getMean());
        }
        anomalyLog.info(anomaly, "Net change in workload: {} --> {} [{}%]",
                segments[0].getMean(), segments[length-1].getMean(),
                segments[0].percentageIncrease(segments[length - 1]));
    }

    private ChangePointDetector getChangePointDetector(Anomaly anomaly) {
        String cpType = anomaly.getDetectorProperty(WORKLOAD_ANALYZER, null);
        if (Strings.isNullOrEmpty(cpType)) {
            cpType = environment.getProperty(WORKLOAD_ANALYZER, DEFAULT_CHANGE_POINT_DETECTOR);
        }

        switch (cpType) {
            case "PELT":
                return new PELTChangePointDetector(environment.getRService());
            case "BinSeg":
                return new BinSegChangePointDetector(environment.getRService());
            case "CL":
                return new CLChangePointDetector(environment.getRService());
            default:
                throw new IllegalArgumentException("Unknown workload analyzer: " + cpType);
        }
    }

    @Override
    protected void doDestroy() {
    }
}
