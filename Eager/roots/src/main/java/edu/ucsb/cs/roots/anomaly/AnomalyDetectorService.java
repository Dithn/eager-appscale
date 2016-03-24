package edu.ucsb.cs.roots.anomaly;

import com.google.common.collect.ImmutableList;
import edu.ucsb.cs.roots.ManagedService;
import edu.ucsb.cs.roots.RootsEnvironment;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.quartz.SchedulerException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class AnomalyDetectorService extends ManagedService {

    private final AnomalyDetectorScheduler scheduler;
    private final Map<String,AnomalyDetector> detectors = new ConcurrentHashMap<>();

    public AnomalyDetectorService(RootsEnvironment environment) throws SchedulerException {
        super(environment);
        this.scheduler = new AnomalyDetectorScheduler(environment.getId());
    }

    public synchronized void doInit() throws Exception {
        scheduler.init();

        File detectorsDir = new File("conf", "detectors");
        Collection<File> children = FileUtils.listFiles(detectorsDir,
                new String[]{"properties"}, false);
        children.stream().forEach(f -> {
            try {
                buildDetector(f);
            } catch (IOException | SchedulerException e) {
                log.warn("Error while loading detector from: {}", f.getAbsolutePath(), e);
            }
        });
    }

    public synchronized void doDestroy() {
        ImmutableList.copyOf(detectors.values()).forEach(this::cancelDetector);
        detectors.clear();
        try {
            scheduler.destroy();
        } catch (SchedulerException e) {
            log.warn("Error while stopping the scheduler");
        }
    }

    private void cancelDetector(AnomalyDetector detector) {
        try {
            scheduler.cancel(detector);
            detectors.remove(detector.getApplication());
            log.info("Cancelled detector job for: {}", detector.getApplication());
        } catch (SchedulerException e) {
            log.warn("Error while cancelling the detector for: {}", detector.getApplication());
        }
    }

    private void buildDetector(File file) throws IOException, SchedulerException {
        try (FileInputStream in = FileUtils.openInputStream(file)) {
            log.info("Loading detector from: {}", file.getAbsolutePath());
            Properties properties = new Properties();
            properties.load(in);
            String application = FilenameUtils.removeExtension(file.getName());
            AnomalyDetector detector = AnomalyDetectorFactory.create(environment, application, properties);
            scheduler.schedule(detector);
            detectors.put(application, detector);
            log.info("Scheduled detector job for: {}", detector.getApplication());
        }
    }

}
