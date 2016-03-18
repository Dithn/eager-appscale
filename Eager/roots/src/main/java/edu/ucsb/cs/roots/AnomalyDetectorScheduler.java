package edu.ucsb.cs.roots;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkState;

public final class AnomalyDetectorScheduler {

    private static final String ANOMALY_DETECTOR_GROUP = "anomaly-detector";

    private enum State {
        STANDBY,
        INITIALIZED,
        DESTROYED
    }

    private final Scheduler scheduler;
    private State state;

    public AnomalyDetectorScheduler(String id) throws SchedulerException {
        StdSchedulerFactory factory = new StdSchedulerFactory();
        String instanceName = id + "-anomaly-detector-scheduler";
        Properties properties = new Properties();
        properties.setProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, instanceName);
        properties.setProperty(StdSchedulerFactory.PROP_THREAD_POOL_CLASS,
                "org.quartz.simpl.SimpleThreadPool");
        properties.setProperty("org.quartz.threadPool.threadCount", "10");
        factory.initialize(properties);
        checkState(factory.getScheduler(instanceName) == null,
                "Attempting to reuse existing Scheduler");
        scheduler = factory.getScheduler();
        state = State.STANDBY;
    }

    public void init() throws SchedulerException {
        checkState(state == State.STANDBY);
        scheduler.start();
        state = State.INITIALIZED;
    }

    public void destroy() throws SchedulerException {
        checkState(state == State.INITIALIZED);
        scheduler.shutdown(true);
        state = State.DESTROYED;
    }

    public void schedule(AnomalyDetector detector) throws SchedulerException {
        String taskKey = getTaskKey(detector.getApplication());
        JobDetail jobDetail = JobBuilder.newJob(AnomalyDetectorJob.class)
                .withIdentity(taskKey, ANOMALY_DETECTOR_GROUP)
                .build();
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        jobDataMap.put(AnomalyDetectorJob.ANOMALY_DETECTOR_INSTANCE, detector);

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(TriggerKey.triggerKey(taskKey, ANOMALY_DETECTOR_GROUP))
                .withSchedule(getScheduleBuilder(detector.getTimeUnit(), detector.getPeriod()))
                .startNow()
                .build();
        scheduler.scheduleJob(jobDetail, trigger);
    }

    private String getTaskKey(String application) {
        return application + "-job";
    }

    private SimpleScheduleBuilder getScheduleBuilder(TimeUnit timeUnit, int period) {
        SimpleScheduleBuilder builder = SimpleScheduleBuilder.simpleSchedule().repeatForever();
        switch (timeUnit) {
            case HOURS:
                builder.withIntervalInHours(period);
                break;
            case MINUTES:
                builder.withIntervalInMinutes(period);
                break;
            case SECONDS:
                builder.withIntervalInSeconds(period);
                break;
        }
        return builder;
    }
}
