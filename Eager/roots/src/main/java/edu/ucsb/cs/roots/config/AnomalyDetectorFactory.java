package edu.ucsb.cs.roots.config;

import com.google.common.base.Strings;
import edu.ucsb.cs.roots.anomaly.AnomalyDetector;
import edu.ucsb.cs.roots.anomaly.AnomalyDetectorBuilder;
import edu.ucsb.cs.roots.anomaly.CorrelationBasedDetector;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

public class AnomalyDetectorFactory {

    private static final String DETECTOR = "detector";
    private static final String DETECTOR_PERIOD = DETECTOR + ".period";
    private static final String DETECTOR_PERIOD_TIME_UNIT = DETECTOR_PERIOD + ".timeUnit";
    private static final String DETECTOR_DATA_STORE = DETECTOR + ".dataStore";

    private static final String DETECTOR_HISTORY_LENGTH = DETECTOR + ".history";
    private static final String DETECTOR_HISTORY_LENGTH_TIME_UNIT = DETECTOR_HISTORY_LENGTH + ".timeUnit";
    private static final String DETECTOR_CORRELATION_THRESHOLD = DETECTOR + ".correlationThreshold";
    private static final String DETECTOR_DTW_INCREASE_THRESHOLD = DETECTOR + ".dtwIncreaseThreshold";
    private static final String DETECTOR_SCRIPT_DIRECTORY = DETECTOR + ".scriptDirectory";

    public static AnomalyDetector create(String application, Properties properties) {
        String detectorType = properties.getProperty(DETECTOR);
        checkArgument(!Strings.isNullOrEmpty(detectorType), "Detector type is required");

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

        String dataStore = properties.getProperty(DETECTOR_DATA_STORE);
        if (!Strings.isNullOrEmpty(dataStore)) {
            builder.setDataStore(dataStore);
        }
        return builder.setApplication(application).build();
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

}
