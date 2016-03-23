package edu.ucsb.cs.roots.config;

import com.google.common.base.Strings;
import edu.ucsb.cs.roots.anomaly.AnomalyDetector;
import edu.ucsb.cs.roots.anomaly.AnomalyDetectorBuilder;
import edu.ucsb.cs.roots.anomaly.CorrelationBasedDetector;
import edu.ucsb.cs.roots.anomaly.SLOBasedDetector;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

public class AnomalyDetectorFactory {

    private static final String DETECTOR = "detector";
    private static final String DETECTOR_PERIOD = "period";
    private static final String DETECTOR_PERIOD_TIME_UNIT = "timeUnit";
    private static final String DETECTOR_DATA_STORE = "dataStore";

    private static final String DETECTOR_HISTORY_LENGTH = "history";
    private static final String DETECTOR_HISTORY_LENGTH_TIME_UNIT = DETECTOR_HISTORY_LENGTH + ".timeUnit";
    private static final String DETECTOR_CORRELATION_THRESHOLD = "correlationThreshold";
    private static final String DETECTOR_DTW_INCREASE_THRESHOLD = "dtwIncreaseThreshold";
    private static final String DETECTOR_SCRIPT_DIRECTORY = "scriptDirectory";

    private static final String DETECTOR_RESPONSE_TIME_UPPER_BOUND = "responseTimeUpperBound";
    private static final String DETECTOR_SLO_PERCENTAGE = "sloPercentage";
    private static final String DETECTOR_WINDOW_FILL_PERCENTAGE = "windowFillPercentage";
    private static final String DETECTOR_SAMPLING_RATE = "samplingRate";
    private static final String DETECTOR_SAMPLING_RATE_TIME_UNIT = DETECTOR_SAMPLING_RATE + ".timeUnit";

    public static AnomalyDetector create(String application, Properties properties) {
        String detectorType = properties.getProperty(DETECTOR);
        checkArgument(!Strings.isNullOrEmpty(detectorType), "Detector type is required");

        AnomalyDetectorBuilder builder;
        if (CorrelationBasedDetector.class.getSimpleName().equals(detectorType)) {
            builder = initCorrelationBasedDetector(properties);
        } else if (SLOBasedDetector.class.getSimpleName().equals(detectorType)) {
            builder = initSLOBasedDetector(properties);
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

    private static SLOBasedDetector.Builder initSLOBasedDetector(Properties properties) {
        SLOBasedDetector.Builder builder = SLOBasedDetector.newBuilder();
        String historyLength = properties.getProperty(DETECTOR_HISTORY_LENGTH);
        if (!Strings.isNullOrEmpty(historyLength)) {
            TimeUnit historyTimeUnit = TimeUnit.valueOf(properties.getProperty(
                    DETECTOR_HISTORY_LENGTH_TIME_UNIT, "SECONDS"));
            builder.setHistoryLengthInSeconds((int) historyTimeUnit.toSeconds(
                    Integer.parseInt(historyLength)));
        }

        String responseTimeUpperBound = properties.getProperty(DETECTOR_RESPONSE_TIME_UPPER_BOUND);
        checkArgument(!Strings.isNullOrEmpty(responseTimeUpperBound),
                "Response time upper bound is required");
        builder.setResponseTimeUpperBound(Integer.parseInt(responseTimeUpperBound));

        String sloPercentage = properties.getProperty(DETECTOR_SLO_PERCENTAGE);
        if (!Strings.isNullOrEmpty(sloPercentage)) {
            builder.setSloPercentage(Double.parseDouble(sloPercentage));
        }

        String windowFillPercentage = properties.getProperty(DETECTOR_WINDOW_FILL_PERCENTAGE);
        if (!Strings.isNullOrEmpty(windowFillPercentage)) {
            builder.setWindowFillPercentage(Double.parseDouble(windowFillPercentage));
        }

        String samplingRate = properties.getProperty(DETECTOR_SAMPLING_RATE);
        if (!Strings.isNullOrEmpty(samplingRate)) {
            TimeUnit samplingTimeUnit = TimeUnit.valueOf(properties.getProperty(
                    DETECTOR_SAMPLING_RATE_TIME_UNIT, "SECONDS"));
            builder.setSamplingIntervalInSeconds((int) samplingTimeUnit.toSeconds(
                    Integer.parseInt(samplingRate)));
        }
        return builder;
    }

}
