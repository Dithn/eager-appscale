package edu.ucsb.cs.roots;


import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws Exception {
        AnomalyDetector detector = new TestAnomalyDetector("watchtower", 2, TimeUnit.SECONDS);
        AnomalyDetectorScheduler scheduler = new AnomalyDetectorScheduler("Test");
        scheduler.init();
        scheduler.schedule(detector);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }
        scheduler.destroy();
    }

}
