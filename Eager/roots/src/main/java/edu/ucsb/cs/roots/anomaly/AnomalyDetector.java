package edu.ucsb.cs.roots.anomaly;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;

public abstract class AnomalyDetector {

    protected final String application;
    protected final int periodInSeconds;
    protected final String dataStore;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    public AnomalyDetector(String application, int periodInSeconds, String dataStore) {
        checkArgument(!Strings.isNullOrEmpty(application), "Application name is required");
        checkArgument(periodInSeconds > 0, "Period must be a positive integer");
        checkArgument(!Strings.isNullOrEmpty(dataStore), "DataStore name is required");
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

    public abstract void run(long now);

}
