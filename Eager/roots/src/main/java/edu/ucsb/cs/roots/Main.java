package edu.ucsb.cs.roots;


import com.google.common.collect.ImmutableList;
import edu.ucsb.cs.roots.anomaly.AnomalyDetector;
import edu.ucsb.cs.roots.anomaly.AnomalyDetectorScheduler;
import edu.ucsb.cs.roots.config.AnomalyDetectorFactory;
import edu.ucsb.cs.roots.utils.ImmutableCollectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.Properties;

public class Main {

    public static void main(String[] args) throws Exception {
        AnomalyDetectorScheduler scheduler = new AnomalyDetectorScheduler("Test");
        scheduler.init();

        File confDir = new File("conf");
        Collection<File> children = FileUtils.listFiles(confDir, new String[]{"properties"}, false);
        ImmutableList<AnomalyDetector> detectors = children.stream()
                .filter(c -> !c.getName().startsWith("_roots_"))
                .map(Main::buildDetector)
                .filter(Objects::nonNull).collect(ImmutableCollectors.toList());
        for (AnomalyDetector detector : detectors) {
            scheduler.schedule(detector);
        }
        try {
            Thread.sleep(60000);
        } catch (InterruptedException ignored) {
        }
        scheduler.destroy();
    }

    private static AnomalyDetector buildDetector(File file) {
        Properties properties = new Properties();
        try (FileInputStream in = FileUtils.openInputStream(file)) {
            properties.load(in);
            String application = FilenameUtils.removeExtension(file.getName());
            return AnomalyDetectorFactory.create(application, properties);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

}
