package edu.ucsb.cs.roots.anomaly;

import edu.ucsb.cs.roots.ConfigLoader;
import edu.ucsb.cs.roots.RootsEnvironment;
import org.junit.Test;

import java.util.Properties;
import java.util.stream.Stream;

public class PathAnomalyDetectorTest {
    
    public void testPathAnomalyDetector() throws Exception {
        RootsEnvironment environment = new RootsEnvironment("Test", new ConfigLoader() {
            @Override
            public Stream<ItemConfig> loadItems(int type, boolean ignoreFaults) {
                if (type == ConfigLoader.DATA_STORES) {
                    Properties properties = new Properties();
                    properties.setProperty("type", "RandomDataStore");
                    return Stream.of(new ItemConfig("default", properties));
                }
                return Stream.empty();
            }
        });
        environment.init();
        try {
            PathAnomalyDetector detector = PathAnomalyDetector.newBuilder()
                    .setPeriodInSeconds(1)
                    .setHistoryLengthInSeconds(10)
                    .setApplication("app")
                    .build(environment);
            for (int i = 0; i < 20; i++) {
                detector.run(100000);
                Thread.sleep(1000);
            }
        } finally {
            environment.destroy();
        }
    }

}
