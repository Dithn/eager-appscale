package edu.ucsb.cs.roots.anomaly;

import com.google.common.base.Strings;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.scheduling.ScheduledItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AnomalyDetector extends ScheduledItem {

    protected final RootsEnvironment environment;
    protected final int historyLengthInSeconds;
    protected final String dataStore;
    protected final Properties properties;
    protected final boolean enableWaiting;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<String,WaitPeriod> waitPeriods = new HashMap<>();

    public AnomalyDetector(RootsEnvironment environment, AnomalyDetectorBuilder builder) {
        super(builder.application, builder.periodInSeconds);
        checkNotNull(environment, "Environment must not be null");
        checkArgument(builder.historyLengthInSeconds > 0, "Period must be a positive integer");
        checkArgument(builder.historyLengthInSeconds % builder.periodInSeconds == 0,
                "History length must be a multiple of period");
        checkArgument(!Strings.isNullOrEmpty(builder.dataStore), "DataStore name is required");
        checkNotNull(builder.properties, "Properties must not be null");
        this.environment = environment;
        this.historyLengthInSeconds = builder.historyLengthInSeconds;
        this.dataStore = builder.dataStore;
        this.enableWaiting = builder.enableWaiting;
        this.properties = builder.properties;
    }

    public final String getDataStore() {
        return dataStore;
    }

    public final String getProperty(String key, String def) {
        return properties.getProperty(key, def);
    }

    protected final boolean isWaiting(String operation, long now) {
        WaitPeriod waitPeriod = waitPeriods.get(operation);
        return waitPeriod != null && waitPeriod.isWaiting(now);
    }

    protected final long getLastAnomalyTime(String operation) {
        WaitPeriod waitPeriod = waitPeriods.get(operation);
        if (waitPeriod != null) {
            return waitPeriod.from;
        }
        return -1L;
    }

    protected final void reportAnomaly(long start, long end, int type,
                                       String operation, String description) {
        if (enableWaiting) {
            if (isWaiting(operation, end)) {
                log.info("Wait period in progress for {}:{}", application, operation);
                return;
            }
            long waitDuration = getWaitDuration(operation);
            if (waitDuration > 0) {
                WaitPeriod waitPeriod = new WaitPeriod(end, waitDuration);
                log.info("Setting wait period for {}; expires at: {}", operation,
                        new Date(waitPeriod.to));
                waitPeriods.put(operation, waitPeriod);
            }
        }
        environment.publishEvent(new Anomaly(this, start, end, type, operation, description));
    }

    protected long getWaitDuration(String operation) {
        return -1L;
    }

    private static final class WaitPeriod {
        private final long from;
        private final long to;

        private WaitPeriod(long from, long duration) {
            checkArgument(from > 0, "from must be positive");
            checkArgument(duration > 0, "duration must be positive");
            this.from = from;
            this.to = from + duration;
        }

        private boolean isWaiting(long now) {
            return now < to;
        }
    }

}
