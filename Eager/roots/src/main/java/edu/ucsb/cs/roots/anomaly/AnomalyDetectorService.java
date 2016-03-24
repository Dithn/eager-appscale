package edu.ucsb.cs.roots.anomaly;

import com.google.common.collect.ImmutableList;
import edu.ucsb.cs.roots.ManagedService;
import edu.ucsb.cs.roots.RootsEnvironment;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkState;

public class AnomalyDetectorService extends ManagedService {

    private static final String ANOMALY_DETECTOR_GROUP = "anomaly-detector";

    private final Scheduler scheduler;
    private final Map<String,AnomalyDetector> detectors = new ConcurrentHashMap<>();

    public AnomalyDetectorService(RootsEnvironment environment) throws SchedulerException {
        super(environment);
        this.scheduler = initScheduler();
    }

    public synchronized void doInit() throws Exception {
        scheduler.start();

        File detectorsDir = new File("conf", "detectors");
        Collection<File> children = FileUtils.listFiles(detectorsDir,
                new String[]{"properties"}, false);
        children.stream().forEach(f -> {
            try {
                scheduleDetector(f);
            } catch (IOException | SchedulerException e) {
                log.warn("Error while loading detector from: {}", f.getAbsolutePath(), e);
            }
        });
    }

    public synchronized void doDestroy() {
        ImmutableList.copyOf(detectors.values()).forEach(this::cancelDetector);
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
                "org.quartz.simpl.SimpleThreadPool");
        properties.setProperty("org.quartz.threadPool.threadCount", "10");
        factory.initialize(properties);
        checkState(factory.getScheduler(instanceName) == null,
                "Attempting to reuse existing Scheduler");
        return factory.getScheduler();
    }

    private void cancelDetector(AnomalyDetector detector) {
        try {
            scheduler.unscheduleJob(getTriggerKey(detector.getApplication()));
            detectors.remove(detector.getApplication());
            log.info("Cancelled detector job for: {}", detector.getApplication());
        } catch (SchedulerException e) {
            log.warn("Error while cancelling the detector for: {}", detector.getApplication());
        }
    }

    private void scheduleDetector(File file) throws IOException, SchedulerException {
        Properties properties = new Properties();
        try (FileInputStream in = FileUtils.openInputStream(file)) {
            log.info("Loading detector configuration from: {}", file.getAbsolutePath());
            properties.load(in);
        }

        String application = FilenameUtils.removeExtension(file.getName());
        AnomalyDetector detector = AnomalyDetectorFactory.create(environment,
                application, properties);
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
        detectors.put(application, detector);
        log.info("Scheduled detector job for: {}", detector.getApplication());
    }

}
