package edu.ucsb.cs.roots.anomaly;

import com.google.common.base.Strings;
import edu.ucsb.cs.roots.RootsEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AnomalyDetector {

    protected final RootsEnvironment environment;
    protected final String application;
    protected final int periodInSeconds;
    protected final String dataStore;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    public AnomalyDetector(RootsEnvironment environment, String application,
                           int periodInSeconds, String dataStore) {
        checkNotNull(environment, "Environment must not be null");
        checkArgument(!Strings.isNullOrEmpty(application), "Application name is required");
        checkArgument(periodInSeconds > 0, "Period must be a positive integer");
        checkArgument(!Strings.isNullOrEmpty(dataStore), "DataStore name is required");
        this.environment = environment;
        this.application = application;
        this.periodInSeconds = periodInSeconds;
        this.dataStore = dataStore;
    }

    public final String getApplication() {
        return application;
    }

    public final int getPeriodInSeconds() {
        return periodInSeconds;
    }

    public String getDataStore() {
        return dataStore;
    }

    public abstract void run(long now);

    protected final void reportAnomaly(long start, long end, String key, String description) {
        environment.publishEvent(new Anomaly(start, end, application, key, description));
    }

}
