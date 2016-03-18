package edu.ucsb.cs.roots;

import com.google.common.base.Strings;
import org.quartz.*;

import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AnomalyDetector {

    private final String application;
    private final int period;
    private final TimeUnit timeUnit;

    public AnomalyDetector(String application, int period, TimeUnit timeUnit) {
        checkArgument(!Strings.isNullOrEmpty(application));
        checkArgument(timeUnit == TimeUnit.HOURS || timeUnit == TimeUnit.MINUTES
                || timeUnit == TimeUnit.SECONDS);
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
