package edu.ucsb.cs.roots.anomaly;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public final class AnomalyLogger {

    private static final Logger log = LoggerFactory.getLogger(AnomalyLogger.class);

    AnomalyLogger() {
    }

    @Subscribe
    public void log(Anomaly anomaly) {
        log.warn("Anomaly detected in {} ({}) at {}: {}", anomaly.getApplication(),
                anomaly.getOperation(), new Date(anomaly.getTimestamp()), anomaly.getDescription());
    }

}
