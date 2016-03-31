package edu.ucsb.cs.roots.bm;

import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.scheduling.SchedulerService;

import java.util.Properties;

public class BenchmarkingService extends SchedulerService<Benchmark> {

    private static final String BENCHMARK_GROUP = "benchmark";
    private static final String BENCHMARK_THREAD_POOL = "benchmark.threadPool";
    private static final String BENCHMARK_THREAD_COUNT = "benchmark.threadCount";

    public BenchmarkingService(RootsEnvironment environment) {
        super(environment, environment.getId() + "-anomaly-detector-scheduler",
                environment.getProperty(BENCHMARK_THREAD_POOL, "org.quartz.simpl.SimpleThreadPool"),
                Integer.parseInt(environment.getProperty(BENCHMARK_THREAD_COUNT, "10")),
                BENCHMARK_GROUP);
    }

    @Override
    protected Benchmark createItem(RootsEnvironment environment, Properties properties) {
        return Benchmark.create(properties);
    }
}
