package edu.ucsb.cs.roots;

import com.google.common.collect.ImmutableList;
import edu.ucsb.cs.roots.anomaly.AnomalyDetector;
import edu.ucsb.cs.roots.anomaly.AnomalyDetectorScheduler;
import edu.ucsb.cs.roots.config.AnomalyDetectorFactory;
import edu.ucsb.cs.roots.config.DataStoreManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

import static com.google.common.base.Preconditions.checkState;

public final class Roots {

    private static final Logger log = LoggerFactory.getLogger(Roots.class);

    private final List<AnomalyDetector> detectors;
    private final AnomalyDetectorScheduler scheduler;
    private State state;

    public static void main(String[] args) throws SchedulerException {
        Roots roots = new Roots();
        roots.start();

        Runtime.getRuntime().addShutdownHook(new Thread("RootsShutdownHook") {
            @Override
            public void run() {
                roots.stop();
            }
        });
        roots.waitFor();
    }

    public Roots() throws SchedulerException {
        this.detectors = new ArrayList<>();
        this.scheduler = new AnomalyDetectorScheduler("Roots");
        this.state = State.STANDBY;
    }

    public synchronized void start() throws SchedulerException {
        checkState(state == State.STANDBY);
        log.info("Starting Roots...");
        DataStoreManager.getInstance().init();
        scheduler.init();

        File detectorsDir = new File("conf", "detectors");
        Collection<File> children = FileUtils.listFiles(detectorsDir, new String[]{"properties"}, false);
        children.stream().map(this::buildDetector)
                .filter(Objects::nonNull)
                .forEach(this::scheduleDetector);
        state = State.INITIALIZED;
    }

    public synchronized void stop() {
        checkState(state == State.INITIALIZED);
        log.info("Stopping Roots...");
        ImmutableList.copyOf(detectors).forEach(this::cancelDetector);
        try {
            scheduler.destroy();
        } catch (SchedulerException e) {
            log.warn("Error while stopping the scheduler");
        }

        try {
            DataStoreManager.getInstance().destroy();
            state = State.DESTROYED;
        } finally {
            this.notifyAll();
        }
    }

    public synchronized void waitFor() {
        while (state == State.INITIALIZED) {
            try {
                this.wait(10000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void scheduleDetector(AnomalyDetector detector) {
        try {
            scheduler.schedule(detector);
            detectors.add(detector);
            log.info("Scheduled detector job for: {}", detector.getApplication());
        } catch (SchedulerException e) {
            log.warn("Error while scheduling the detector for: {}", detector.getApplication());
        }
    }

    private void cancelDetector(AnomalyDetector detector) {
        try {
            scheduler.cancel(detector);
            detectors.remove(detector);
            log.info("Cancelled detector job for: {}", detector.getApplication());
        } catch (SchedulerException e) {
            log.warn("Error while cancelling the detector for: {}", detector.getApplication());
        }
    }

    private AnomalyDetector buildDetector(File file) {
        try (FileInputStream in = FileUtils.openInputStream(file)) {
            log.info("Loading detector from: {}", file.getAbsolutePath());
            Properties properties = new Properties();
            properties.load(in);
            String application = FilenameUtils.removeExtension(file.getName());
            return AnomalyDetectorFactory.create(application, properties);
        } catch (Exception e) {
            log.error("Error while loading detector from: {}", file.getAbsolutePath(), e);
            return null;
        }
    }
}
