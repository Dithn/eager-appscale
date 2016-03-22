package edu.ucsb.cs.roots.config;

import com.google.common.base.Strings;
import edu.ucsb.cs.roots.anomaly.AnomalyDetector;
import edu.ucsb.cs.roots.anomaly.AnomalyDetectorBuilder;
import edu.ucsb.cs.roots.anomaly.CorrelationBasedDetector;
import edu.ucsb.cs.roots.data.DataStore;
import edu.ucsb.cs.roots.data.ElasticSearchDataStore;
import edu.ucsb.cs.roots.data.TestDataStore;

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
    private static final String DATA_STORE_ES_HOST = DATA_STORE + ".es.host";
    private static final String DATA_STORE_ES_PORT = DATA_STORE + ".es.port";
    private static final String DATA_STORE_ES_ACCESS_LOG_INDEX = DATA_STORE + ".es.accessLog.index";

    public static AnomalyDetector create(String application, Properties properties) {
        String detectorType = getRequired(properties, DETECTOR);

        AnomalyDetectorBuilder builder;
        if (CorrelationBasedDetector.class.getSimpleName().equals(detectorType)) {
            builder = initCorrelationBasedDetector(properties);
        } else {
            throw new IllegalArgumentException("Unknown anomaly detector type: " + detectorType);
        }

        String period = properties.getProperty(DETECTOR_PERIOD);
        if (!Strings.isNullOrEmpty(period)) {
            TimeUnit timeUnit = TimeUnit.valueOf(properties.getProperty(
                    DETECTOR_PERIOD_TIME_UNIT, "SECONDS"));
            builder.setPeriodInSeconds((int) timeUnit.toSeconds(Integer.parseInt(period)));
        }

        DataStore dataStore;
        String dataStoreType = properties.getProperty(DATA_STORE);
        if (Strings.isNullOrEmpty(dataStoreType)) {
            dataStore = DefaultDataStore.getInstance().get();
        } else {
            dataStore = initDataStore(properties);
        }

        return builder.setApplication(application)
                .setDataStore(dataStore)
                .build();
    }

    public static DataStore initDataStore(Properties properties) {
        String dataStore = getRequired(properties, DATA_STORE);
        if (TestDataStore.class.getSimpleName().equals(dataStore)) {
            return new TestDataStore();
        } else if (ElasticSearchDataStore.class.getSimpleName().equals(dataStore)) {
            return ElasticSearchDataStore.newBuilder()
                    .setElasticSearchHost(getRequired(properties, DATA_STORE_ES_HOST))
                    .setElasticSearchPort(getRequiredInt(properties, DATA_STORE_ES_PORT))
                    .setAccessLogIndex(getRequired(properties, DATA_STORE_ES_ACCESS_LOG_INDEX))
                    .build();
        } else {
            throw new IllegalArgumentException("Unknown data store type: " + dataStore);
        }
    }

    private static CorrelationBasedDetector.Builder initCorrelationBasedDetector(
            Properties properties) {
        CorrelationBasedDetector.Builder builder = CorrelationBasedDetector.newBuilder();
        String historyLength = properties.getProperty(DETECTOR_HISTORY_LENGTH);
        if (!Strings.isNullOrEmpty(historyLength)) {
            TimeUnit historyTimeUnit = TimeUnit.valueOf(properties.getProperty(
                    DETECTOR_HISTORY_LENGTH_TIME_UNIT, "SECONDS"));
            builder.setHistoryLengthInSeconds((int) historyTimeUnit.toSeconds(
                    Integer.parseInt(historyLength)));
        }

        String correlationThreshold = properties.getProperty(DETECTOR_CORRELATION_THRESHOLD);
        if (!Strings.isNullOrEmpty(correlationThreshold)) {
            builder.setCorrelationThreshold(Double.parseDouble(correlationThreshold));
        }

        String dtwIncreaseThreshold = properties.getProperty(DETECTOR_DTW_INCREASE_THRESHOLD);
        if (!Strings.isNullOrEmpty(dtwIncreaseThreshold)) {
            builder.setDtwIncreaseThreshold(Double.parseDouble(dtwIncreaseThreshold));
        }

        String scriptDirectory = properties.getProperty(DETECTOR_SCRIPT_DIRECTORY);
        if (!Strings.isNullOrEmpty(scriptDirectory)) {
            builder.setScriptDirectory(scriptDirectory);
        }
        return builder;
    }

    private static String getRequired(Properties properties, String name) {
        String value = properties.getProperty(name);
        checkArgument(!Strings.isNullOrEmpty(value), "Property %s is required", name);
        return value;
    }

    private static int getRequiredInt(Properties properties, String name) {
        String value = properties.getProperty(name);
        checkArgument(!Strings.isNullOrEmpty(value), "Property %s is required", name);
        return Integer.parseInt(value);
    }

}
