package edu.ucsb.cs.roots.anomaly;

import com.google.common.eventbus.Subscribe;
import org.slf4j.LoggerFactory;

import java.util.Date;

public final class AnomalyLogger {

    private final AnomalyLog anomalyLog;

    AnomalyLogger() {
        this.anomalyLog = new AnomalyLog(LoggerFactory.getLogger(AnomalyLogger.class));
    }

    @Subscribe
    public void log(Anomaly anomaly) {
        anomalyLog.warn(anomaly, "Detected at {}: {}", new Date(anomaly.getEnd()),
                anomaly.getDescription());
    }

}
