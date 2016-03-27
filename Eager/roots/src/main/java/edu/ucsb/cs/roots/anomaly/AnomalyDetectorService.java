package edu.ucsb.cs.roots.anomaly;

import com.google.common.collect.ImmutableList;
import edu.ucsb.cs.roots.ConfigLoader;
import edu.ucsb.cs.roots.ManagedService;
import edu.ucsb.cs.roots.RootsEnvironment;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkState;

public class AnomalyDetectorService extends ManagedService {

    private static final String ANOMALY_DETECTOR_GROUP = "anomaly-detector";
    private static final String QUARTZ_THREAD_POOL = "quartz.threadPool";
    private static final String QUARTZ_THREAD_COUNT = "quartz.threadCount";

    private Scheduler scheduler;
    private final List<AnomalyDetector> detectors = new ArrayList<>();

    public AnomalyDetectorService(RootsEnvironment environment) throws SchedulerException {
        super(environment);
    }

    public synchronized void doInit() throws Exception {
        environment.subscribe(new AnomalyLogger());
        scheduler = initScheduler();
        scheduler.start();
        environment.getConfigLoader().loadItems(ConfigLoader.DETECTORS, true).forEach(p -> {
            try {
                scheduleDetector(p);
            } catch (Exception e) {
                log.warn("Error while scheduling detector", e);
            }
        });
    }

    public synchronized void doDestroy() {
        ImmutableList.copyOf(detectors).forEach(this::cancelDetector);
        detectors.clear();
        try {
            scheduler.shutdown(true);
        } catch (SchedulerException e) {
            log.warn("Error while stopping the scheduler");
        }
    }

    private JobKey getJobKey(String application) {
        return JobKey.jobKey(application + "-job", ANOMALY_DETECTOR_GROUP);
    }

    private TriggerKey getTriggerKey(String application) {
        return TriggerKey.triggerKey(application + "-job", ANOMALY_DETECTOR_GROUP);
    }

    private Scheduler initScheduler() throws SchedulerException {
        StdSchedulerFactory factory = new StdSchedulerFactory();
        String instanceName = environment.getId() + "-anomaly-detector-scheduler";
        Properties properties = new Properties();
        properties.setProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, instanceName);
        properties.setProperty(StdSchedulerFactory.PROP_THREAD_POOL_CLASS,
                environment.getProperty(QUARTZ_THREAD_POOL, "org.quartz.simpl.SimpleThreadPool"));
        properties.setProperty("org.quartz.threadPool.threadCount",
                environment.getProperty(QUARTZ_THREAD_COUNT, "10"));
        factory.initialize(properties);
        checkState(factory.getScheduler(instanceName) == null,
                "Attempting to reuse existing Scheduler");
        return factory.getScheduler();
    }

    private void cancelDetector(AnomalyDetector detector) {
        try {
            scheduler.unscheduleJob(getTriggerKey(detector.getApplication()));
            detectors.remove(detector);
            log.info("Cancelled detector job for: {}", detector.getApplication());
        } catch (SchedulerException e) {
            log.warn("Error while cancelling the detector for: {}", detector.getApplication());
        }
    }

    private void scheduleDetector(Properties properties) throws SchedulerException {
        AnomalyDetector detector = AnomalyDetectorFactory.create(environment, properties);
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
        detectors.add(detector);
        log.info("Scheduled detector job for: {}", detector.getApplication());
    }

}
