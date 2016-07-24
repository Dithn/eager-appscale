package edu.ucsb.cs.roots.anomaly;

import edu.ucsb.cs.roots.ConfigLoader;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.data.TestDataStore;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
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

            Assert.assertFalse(detector.isWaiting("foo", 75000));
            detector.run(75000);
            Assert.assertEquals(1, recorder.getAndClearAnomalies().size());

            Assert.assertFalse(detector.isWaiting("foo", 80000));
            detector.run(80000);
            Assert.assertEquals(1, recorder.getAndClearAnomalies().size());
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
            List<Anomaly> anomalies = recorder.getAndClearAnomalies();
            Assert.assertEquals(1, anomalies.size());

            Assert.assertTrue(detector.isWaiting("foo", 75000));
            detector.run(75000);
            Assert.assertEquals(0, recorder.getAndClearAnomalies().size());

            Assert.assertFalse(detector.isWaiting("foo", 80000));
            detector.run(80000);
            anomalies = recorder.getAndClearAnomalies();
            Assert.assertEquals(1, anomalies.size());
        } finally {
            environment.destroy();
        }
    }

    @Test
    public void testNewAnomaly() throws Exception {
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
                            d.reportAnomaly(d.newAnomaly(1, now, "foo")
                                    .setType(Anomaly.TYPE_PERFORMANCE)
                                    .setDescription("test")
                                    .build()))
                    .setEnableWaiting(true)
                    .setWaitDuration(10 * 1000L)
                    .build(environment);

            detector.run(70000);
            List<Anomaly> anomalies = recorder.getAndClearAnomalies();
            Assert.assertEquals(1, anomalies.size());
            Anomaly anomaly = anomalies.get(0);
            Assert.assertEquals(1, anomaly.getStart());
            Assert.assertEquals(70000, anomaly.getEnd());
            Assert.assertEquals("foo", anomaly.getOperation());
            Assert.assertEquals(-1L, anomaly.getPreviousAnomalyTime());


            Assert.assertTrue(detector.isWaiting("foo", 75000));
            detector.run(75000);
            Assert.assertEquals(0, recorder.getAndClearAnomalies().size());

            Assert.assertFalse(detector.isWaiting("foo", 80000));
            detector.run(80000);
            anomalies = recorder.getAndClearAnomalies();
            Assert.assertEquals(1, anomalies.size());
            anomaly = anomalies.get(0);
            Assert.assertEquals(1, anomaly.getStart());
            Assert.assertEquals(80000, anomaly.getEnd());
            Assert.assertEquals("foo", anomaly.getOperation());
            Assert.assertEquals(70000, anomaly.getPreviousAnomalyTime());
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
