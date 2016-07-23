package edu.ucsb.cs.roots.anomaly;

import edu.ucsb.cs.roots.ConfigLoader;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.data.TestDataStore;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

public class AnomalyDetectorTest {

    @Test
    public void testNoWaitDuration() throws Exception {
        RootsEnvironment environment = getEnvironment();
        try {
            environment.init();
            AnomalyRecorder recorder = new AnomalyRecorder();
            environment.subscribe(recorder);
            TestDataStore dataStore = new TestDataStore(environment, "default");
            TestAnomalyDetector detector = TestAnomalyDetector.newBuilder()
                    .setApplication("test-app")
                    .setPeriodInSeconds(1)
                    .setHistoryLengthInSeconds(5)
                    .setDataStore(dataStore.getName())
                    .setFunction((now,d) ->
                            d.reportAnomaly(Anomaly.newBuilder().setStart(1).setEnd(now)
                                    .setDetector(d).setType(Anomaly.TYPE_PERFORMANCE)
                                    .setOperation("foo").setDescription("test")
                                    .build()))
                    .setWaitDuration(10 * 1000L)
                    .build(environment);

            detector.run(70000);
            Assert.assertEquals(1, recorder.getAndClearAnomalies().size());
            Assert.assertEquals(-1L, detector.getLastAnomalyTime("foo"));

            Assert.assertFalse(detector.isWaiting("foo", 75000));
            detector.run(75000);
            Assert.assertEquals(1, recorder.getAndClearAnomalies().size());
            Assert.assertEquals(-1L, detector.getLastAnomalyTime("foo"));

            Assert.assertFalse(detector.isWaiting("foo", 80000));
            detector.run(80000);
            Assert.assertEquals(1, recorder.getAndClearAnomalies().size());
            Assert.assertEquals(-1L, detector.getLastAnomalyTime("foo"));
        } finally {
            environment.destroy();
        }
    }

    @Test
    public void testWaitDuration() throws Exception {
        RootsEnvironment environment = getEnvironment();
        try {
            environment.init();
            AnomalyRecorder recorder = new AnomalyRecorder();
            environment.subscribe(recorder);
            TestDataStore dataStore = new TestDataStore(environment, "default");
            TestAnomalyDetector detector = TestAnomalyDetector.newBuilder()
                    .setApplication("test-app")
                    .setPeriodInSeconds(1)
                    .setHistoryLengthInSeconds(5)
                    .setDataStore(dataStore.getName())
                    .setFunction((now,d) ->
                            d.reportAnomaly(Anomaly.newBuilder().setStart(1).setEnd(now)
                                    .setDetector(d).setType(Anomaly.TYPE_PERFORMANCE)
                                    .setOperation("foo").setDescription("test")
                                    .build()))
                    .setEnableWaiting(true)
                    .setWaitDuration(10 * 1000L)
                    .build(environment);

            detector.run(70000);
            Assert.assertEquals(1, recorder.getAndClearAnomalies().size());
            Assert.assertEquals(70000, detector.getLastAnomalyTime("foo"));

            Assert.assertTrue(detector.isWaiting("foo", 75000));
            detector.run(75000);
            Assert.assertEquals(0, recorder.getAndClearAnomalies().size());
            Assert.assertEquals(70000, detector.getLastAnomalyTime("foo"));

            Assert.assertFalse(detector.isWaiting("foo", 80000));
            detector.run(80000);
            Assert.assertEquals(1, recorder.getAndClearAnomalies().size());
            Assert.assertEquals(80000, detector.getLastAnomalyTime("foo"));
        } finally {
            environment.destroy();
        }
    }

    private RootsEnvironment getEnvironment() throws Exception {
        return new RootsEnvironment("Test", new ConfigLoader(){
            @Override
            public Properties loadGlobalProperties() throws Exception {
                Properties properties = new Properties();
                properties.setProperty(RootsEnvironment.EVENT_BUS_TYPE, "sync");
                return properties;
            }
        });
    }

}
