package edu.ucsb.cs.roots.anomaly;

import com.google.common.base.Strings;
import edu.ucsb.cs.roots.RootsEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AnomalyDetector {

    protected final RootsEnvironment environment;
    protected final String application;
    protected final int periodInSeconds;
    protected final int historyLengthInSeconds;
    protected final String dataStore;
    protected final Properties properties;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    public AnomalyDetector(RootsEnvironment environment, String application,
                           int periodInSeconds, int historyLengthInSeconds, String dataStore,
                           Properties properties) {
        checkNotNull(environment, "Environment must not be null");
        checkArgument(!Strings.isNullOrEmpty(application), "Application name is required");
        checkArgument(periodInSeconds > 0, "Period must be a positive integer");
        checkArgument(historyLengthInSeconds > 0, "Period must be a positive integer");
        checkArgument(historyLengthInSeconds % periodInSeconds == 0,
                "History length must be a multiple of period");
        checkArgument(!Strings.isNullOrEmpty(dataStore), "DataStore name is required");
        checkNotNull(properties, "Properties must not be null");
        this.environment = environment;
        this.application = application;
        this.periodInSeconds = periodInSeconds;
        this.historyLengthInSeconds = historyLengthInSeconds;
        this.dataStore = dataStore;
        this.properties = properties;
    }

    public final String getApplication() {
        return application;
    }

    public final int getPeriodInSeconds() {
        return periodInSeconds;
    }

    public final String getDataStore() {
        return dataStore;
    }

    public final String getProperty(String key, String def) {
        return properties.getProperty(key, def);
    }

    abstract void run(long now);

    protected final void reportAnomaly(long start, long end, String key, String description) {
        environment.publishEvent(new Anomaly(this, start, end, key, description));
    }

}
