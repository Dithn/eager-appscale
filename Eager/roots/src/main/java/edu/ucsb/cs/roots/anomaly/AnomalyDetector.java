package edu.ucsb.cs.roots.anomaly;

import com.google.common.base.Strings;
import edu.ucsb.cs.roots.data.DataStore;

import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AnomalyDetector {

    protected final String application;
    protected final int period;
    protected final TimeUnit timeUnit;
    protected final DataStore dataStore;

    public AnomalyDetector(String application, int period, TimeUnit timeUnit, DataStore dataStore) {
        checkArgument(!Strings.isNullOrEmpty(application), "Application name is required");
        checkArgument(period > 0, "Period must be a positive integer");
        checkArgument(timeUnit == TimeUnit.HOURS || timeUnit == TimeUnit.MINUTES
                || timeUnit == TimeUnit.SECONDS, "Only hours, minutes and seconds are allowed");
        checkNotNull(dataStore, "DataStore must not be null");
        this.application = application;
        this.period = period;
        this.timeUnit = timeUnit;
        this.dataStore = dataStore;
    }

    public final String getApplication() {
        return application;
    }

    public final int getPeriod() {
        return period;
    }

    public final TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public abstract void run();

}
