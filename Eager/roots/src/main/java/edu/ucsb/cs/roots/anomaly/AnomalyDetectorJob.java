package edu.ucsb.cs.roots.anomaly;

import org.quartz.*;

import static com.google.common.base.Preconditions.checkNotNull;

@DisallowConcurrentExecution
public final class AnomalyDetectorJob implements Job {

    public static final String ANOMALY_DETECTOR_INSTANCE = "anomaly-detector-instance";

    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        AnomalyDetector detector = (AnomalyDetector ) jobDataMap.get(ANOMALY_DETECTOR_INSTANCE);
        checkNotNull(detector);
        detector.run(context.getScheduledFireTime().getTime());
    }

}

