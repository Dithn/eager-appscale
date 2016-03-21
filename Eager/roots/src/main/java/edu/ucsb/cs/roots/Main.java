package edu.ucsb.cs.roots;


import edu.ucsb.cs.roots.anomaly.AnomalyDetector;
import edu.ucsb.cs.roots.anomaly.AnomalyDetectorScheduler;
import edu.ucsb.cs.roots.anomaly.CorrelationBasedDetector;
import edu.ucsb.cs.roots.data.TestDataStore;

public class Main {

    public static void main(String[] args) throws Exception {
        AnomalyDetector detector = CorrelationBasedDetector.newBuilder()
                .setApplication("watchtower")
                .setPeriodInSeconds(1)
                .setDataStore(new TestDataStore())
                .setHistoryLengthInSeconds(10)
                .build();
        AnomalyDetectorScheduler scheduler = new AnomalyDetectorScheduler("Test");
        scheduler.init();
        scheduler.schedule(detector);
        try {
            Thread.sleep(60000);
        } catch (InterruptedException ignored) {
        }
        scheduler.destroy();
    }

}
