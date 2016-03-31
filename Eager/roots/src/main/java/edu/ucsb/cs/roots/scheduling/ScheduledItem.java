package edu.ucsb.cs.roots.scheduling;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;

public abstract class ScheduledItem {

    protected final String application;
    protected final int periodInSeconds;

    public ScheduledItem(String application, int periodInSeconds) {
        checkArgument(!Strings.isNullOrEmpty(application), "Application name is required");
        checkArgument(periodInSeconds > 0, "Period must be a positive integer");
        this.application = application;
        this.periodInSeconds = periodInSeconds;
    }

    public final String getApplication() {
        return application;
    }

    public final int getPeriodInSeconds() {
        return periodInSeconds;
    }

    public abstract void run(long now);
}
