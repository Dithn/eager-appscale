package edu.ucsb.cs.roots.anomaly;

import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.data.DataStoreCall;
import edu.ucsb.cs.roots.data.TestDataStore;
import junit.framework.Assert;
import org.junit.Test;

public class SLOBasedDetectorTest {

    @Test
    public void testDetector() throws Exception {
        RootsEnvironment environment = new RootsEnvironment("Test", "test");
        try {
            environment.init();
            TestDataStore dataStore = new TestDataStore(environment, "test-ds");
            SLOBasedDetector detector = SLOBasedDetector.newBuilder()
                    .setApplication("test-app")
                    .setPeriodInSeconds(1)
                    .setHistoryLengthInSeconds(5)
                    .setSamplingIntervalInSeconds(1)
                    .setResponseTimeUpperBound(20)
                    .setDataStore("test-ds")
                    .build(environment);

            Assert.assertEquals(0, dataStore.callCount());
            detector.run(70000);
            Assert.assertEquals(1, dataStore.callCount());
            DataStoreCall call = dataStore.getCallsAndClear().get(0);
            Assert.assertEquals(TestDataStore.GET_BENCHMARK_RESULTS, call.getType());
            Assert.assertEquals("test-app", call.getApplication());
            Assert.assertEquals(10000L, call.getEnd());
            Assert.assertEquals(5000L, call.getStart());

            Assert.assertEquals(0, dataStore.callCount());
            detector.run(71000);
            Assert.assertEquals(1, dataStore.callCount());
            call = dataStore.getCallsAndClear().get(0);
            Assert.assertEquals(TestDataStore.GET_BENCHMARK_RESULTS, call.getType());
            Assert.assertEquals("test-app", call.getApplication());
            Assert.assertEquals(11000L, call.getEnd());
            Assert.assertEquals(10000L, call.getStart());
        } finally {
            environment.destroy();
        }
    }

}
