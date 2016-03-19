package edu.ucsb.cs.roots;


import edu.ucsb.cs.roots.anomaly.AnomalyDetector;
import edu.ucsb.cs.roots.anomaly.AnomalyDetectorScheduler;
import edu.ucsb.cs.roots.anomaly.CorrelationBasedAnomalyDetector;
import edu.ucsb.cs.roots.data.TestDataStore;

import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws Exception {
        AnomalyDetector detector = CorrelationBasedAnomalyDetector.newBuilder()
                .setApplication("watchtower")
                .setPeriod(1)
                .setTimeUnit(TimeUnit.SECONDS)
                .setDataStore(new TestDataStore())
                .build();
        AnomalyDetectorScheduler scheduler = new AnomalyDetectorScheduler("Test");
        scheduler.init();
        scheduler.schedule(detector);
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
        }
        scheduler.destroy();
    }

}
