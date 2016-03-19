package edu.ucsb.cs.roots.anomaly;

import com.google.common.base.Strings;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

public abstract class AnomalyDetector {

    private final String application;
    private final int period;
    private final TimeUnit timeUnit;

    public AnomalyDetector(String application, int period, TimeUnit timeUnit) {
        checkArgument(!Strings.isNullOrEmpty(application), "Application name is required");
        checkArgument(period > 0, "Period must be a positive integer");
        checkArgument(timeUnit == TimeUnit.HOURS || timeUnit == TimeUnit.MINUTES
                || timeUnit == TimeUnit.SECONDS, "Only hours, minutes and seconds are allowed");
        this.application = application;
        this.period = period;
        this.timeUnit = timeUnit;
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
