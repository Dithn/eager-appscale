package edu.ucsb.cs.roots.config;

import com.google.common.base.Strings;
import edu.ucsb.cs.roots.anomaly.AnomalyDetector;
import edu.ucsb.cs.roots.anomaly.AnomalyDetectorBuilder;
import edu.ucsb.cs.roots.anomaly.CorrelationBasedDetector;
import edu.ucsb.cs.roots.data.DataStore;
import edu.ucsb.cs.roots.data.TestDataStore;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

public class AnomalyDetectorFactory {

    private static final String DETECTOR = "detector";
    private static final String DETECTOR_PERIOD = DETECTOR + ".period";
    private static final String DETECTOR_PERIOD_TIME_UNIT = DETECTOR_PERIOD + ".timeUnit";

    private static final String DETECTOR_HISTORY_LENGTH = DETECTOR + ".history";
    private static final String DETECTOR_HISTORY_LENGTH_TIME_UNIT = DETECTOR_HISTORY_LENGTH + ".timeUnit";
    private static final String DETECTOR_CORRELATION_THRESHOLD = DETECTOR + ".correlationThreshold";
    private static final String DETECTOR_DTW_INCREASE_THRESHOLD = DETECTOR + ".dtwIncreaseThreshold";
    private static final String DETECTOR_SCRIPT_DIRECTORY = DETECTOR + ".scriptDirectory";

    private static final String DATA_STORE = "dataStore";

    public static AnomalyDetector create(String application, Properties properties) {
        String detectorType = properties.getProperty(DETECTOR);
        checkArgument(!Strings.isNullOrEmpty(detectorType), "Detector type is required");
        int period = Integer.parseInt(properties.getProperty(DETECTOR_PERIOD, "60"));
        TimeUnit timeUnit = TimeUnit.valueOf(properties.getProperty(
                DETECTOR_PERIOD_TIME_UNIT, "SECONDS"));
        AnomalyDetectorBuilder builder;
        if (CorrelationBasedDetector.class.getSimpleName().equals(detectorType)) {
            builder = initCorrelationBasedDetector(properties);
        } else {
            throw new IllegalArgumentException("Unknown anomaly detector type: " + detectorType);
        }

        DataStore dataStore;
        String dataStoreType = properties.getProperty(DATA_STORE);
        if (Strings.isNullOrEmpty(dataStoreType)) {
            dataStore = DefaultDataStore.getInstance().get();
        } else {
            dataStore = initDataStore(properties);
        }

        return builder.setPeriodInSeconds((int) timeUnit.toSeconds(period))
                .setApplication(application)
                .setDataStore(dataStore)
                .build();
    }

    public static DataStore initDataStore(Properties properties) {
        String dataStore = properties.getProperty(DATA_STORE);
        checkArgument(!Strings.isNullOrEmpty(dataStore), "Data store type is required");
        if (TestDataStore.class.getSimpleName().equals(dataStore)) {
            return new TestDataStore();
        } else {
            throw new IllegalArgumentException("Unknown data store type: " + dataStore);
        }
    }

    private static CorrelationBasedDetector.Builder initCorrelationBasedDetector(
            Properties properties) {
        CorrelationBasedDetector.Builder builder = CorrelationBasedDetector.newBuilder();
        String historyLength = properties.getProperty(DETECTOR_HISTORY_LENGTH);
        if (historyLength != null) {
            TimeUnit historyTimeUnit = TimeUnit.valueOf(properties.getProperty(
                    DETECTOR_HISTORY_LENGTH_TIME_UNIT, "SECONDS"));
            builder.setHistoryLengthInSeconds((int) historyTimeUnit.toSeconds(
                    Integer.parseInt(historyLength)));
        }

        String correlationThreshold = properties.getProperty(DETECTOR_CORRELATION_THRESHOLD);
        if (correlationThreshold != null) {
            builder.setCorrelationThreshold(Double.parseDouble(correlationThreshold));
        }

        String dtwIncreaseThreshold = properties.getProperty(DETECTOR_DTW_INCREASE_THRESHOLD);
        if (dtwIncreaseThreshold != null) {
            builder.setDtwIncreaseThreshold(Double.parseDouble(dtwIncreaseThreshold));
        }

        String scriptDirectory = properties.getProperty(DETECTOR_SCRIPT_DIRECTORY);
        if (scriptDirectory != null) {
            builder.setScriptDirectory(scriptDirectory);
        }
        return builder;
    }

}
