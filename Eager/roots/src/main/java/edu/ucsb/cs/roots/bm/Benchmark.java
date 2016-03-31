package edu.ucsb.cs.roots.bm;

import com.google.common.base.Strings;
import edu.ucsb.cs.roots.scheduling.ScheduledItem;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

public final class Benchmark extends ScheduledItem {

    private static final String APPLICATION = "application";
    private static final String BENCHMARK_PERIOD = "period";
    private static final String BENCHMARK_PERIOD_TIME_UNIT = "timeUnit";

    public static Benchmark create(Properties properties) {
        String application = properties.getProperty(APPLICATION);
        checkArgument(!Strings.isNullOrEmpty(application), "Application name is required");

        int periodValue;
        String period = properties.getProperty(BENCHMARK_PERIOD);
        if (Strings.isNullOrEmpty(period)) {
            periodValue = Integer.parseInt(period);
        } else {
            TimeUnit timeUnit = TimeUnit.valueOf(properties.getProperty(
                    BENCHMARK_PERIOD_TIME_UNIT, "SECONDS"));
            periodValue = (int) timeUnit.toSeconds(Integer.parseInt(period));
        }
        return new Benchmark(application, periodValue);
    }

    private Benchmark(String application, int periodInSeconds) {
        super(application, periodInSeconds);
    }

    public void run(long now) {

    }
}
