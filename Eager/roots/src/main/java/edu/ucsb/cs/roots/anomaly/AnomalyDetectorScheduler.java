package edu.ucsb.cs.roots.anomaly;

import com.google.common.base.Strings;
import edu.ucsb.cs.roots.State;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Properties;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

public final class AnomalyDetectorScheduler {

    private static final String ANOMALY_DETECTOR_GROUP = "anomaly-detector";

    private final Scheduler scheduler;
    private State state;

    public AnomalyDetectorScheduler(String id) throws SchedulerException {
        checkArgument(!Strings.isNullOrEmpty(id), "ID is required");
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

    public synchronized void init() throws SchedulerException {
        checkState(state == State.STANDBY);
        scheduler.start();
        state = State.INITIALIZED;
    }

    public synchronized void destroy() throws SchedulerException {
        checkState(state == State.INITIALIZED);
        scheduler.shutdown(true);
        state = State.DESTROYED;
    }

    public void schedule(AnomalyDetector detector) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(AnomalyDetectorJob.class)
                .withIdentity(getJobKey(detector.getApplication()))
                .build();
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        jobDataMap.put(AnomalyDetectorJob.ANOMALY_DETECTOR_INSTANCE, detector);

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(getTriggerKey(detector.getApplication()))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withMisfireHandlingInstructionIgnoreMisfires()
                        .repeatForever().withIntervalInSeconds(detector.getPeriodInSeconds()))
                .startNow()
                .build();
        scheduler.scheduleJob(jobDetail, trigger);
    }

    public void cancel(AnomalyDetector detector) throws SchedulerException {
        scheduler.unscheduleJob(getTriggerKey(detector.getApplication()));
    }

    private JobKey getJobKey(String application) {
        return JobKey.jobKey(application + "-job", ANOMALY_DETECTOR_GROUP);
    }

    private TriggerKey getTriggerKey(String application) {
        return TriggerKey.triggerKey(application + "-job", ANOMALY_DETECTOR_GROUP);
    }
}
