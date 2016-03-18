package edu.ucsb.cs.roots;

import java.util.concurrent.TimeUnit;

public class TestAnomalyDetector extends AnomalyDetector {

    public TestAnomalyDetector(String application, int period, TimeUnit timeUnit) {
        super(application, period, timeUnit);
    }

    @Override
    public void run() {
        System.out.println("Running detector");
    }
}
