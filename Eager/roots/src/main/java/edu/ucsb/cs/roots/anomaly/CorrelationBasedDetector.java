package edu.ucsb.cs.roots.anomaly;

import com.google.common.collect.EvictingQueue;
import edu.ucsb.cs.roots.data.ResponseTimeSummary;
import edu.ucsb.cs.roots.utils.CommandLineUtils;
import edu.ucsb.cs.roots.utils.CommandOutput;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Based on "Detection of Performance Anomalies in Web-based Applications" by Magalhaes and Silva.
 * Calculates the correlation between response time and number of requests to detect any
 * anomalous increases in response time. If the correlation drops below a threshold, checks
 * the dynamic time warping (DTW) distance to further ensure that the anomalous performance
 * is not explained by the workload conditions.
 */
public final class CorrelationBasedDetector extends AnomalyDetector {

    private final int historyLength;
    private final Map<String,EvictingQueue<ResponseTimeSummary>> history;
    private final File rDirectory;
    private final double correlationThreshold;
    private final double dtwIncreasePercentageThreshold;

    private long end = -1L;
    private Map<String,Double> prevDtw = new HashMap<>();

    private CorrelationBasedDetector(Builder builder) {
        super(builder.application, builder.period, builder.timeUnit, builder.dataStore);
        checkArgument(builder.historyLength > 10, "History length must be greater than 10");
        checkArgument(builder.correlationThreshold >= -1 && builder.correlationThreshold <= 1,
                "Correlation threshold must be in the interval [-1,1]");
        checkArgument(builder.dtwIncreasePercentageThreshold > 0,
                "DTW increase percentage threshold must be positive");
        this.historyLength = builder.historyLength;
        this.history = new HashMap<>();
        this.rDirectory = new File(builder.rDirectory);
        this.correlationThreshold = builder.correlationThreshold;
        this.dtwIncreasePercentageThreshold = builder.dtwIncreasePercentageThreshold;
        checkArgument(rDirectory.exists(), "R directory path does not exist: %s",
                rDirectory.getAbsolutePath());
        checkArgument(rDirectory.isDirectory(), "%s is not a directory",
                rDirectory.getAbsolutePath());
    }

    @Override
    public void run() {
        long start;
        if (end < 0) {
            end = System.currentTimeMillis() - 60 * 1000;
            start = end - timeUnit.toMillis(period);
        } else {
            start = end;
            end += timeUnit.toMillis(period);
        }

        Map<String,ResponseTimeSummary> summaries = dataStore.getResponseTimeSummary(
                application, start, end);
        history.entrySet().stream()
                .filter(e -> !summaries.containsKey(e.getKey()))
                .forEach(e -> e.getValue().add(ResponseTimeSummary.ZERO));

        for (Map.Entry<String,ResponseTimeSummary> entry : summaries.entrySet()) {
            EvictingQueue<ResponseTimeSummary> record = history.get(entry.getKey());
            if (record == null) {
                record = EvictingQueue.create(historyLength);
                history.put(entry.getKey(), record);
            }
            record.add(entry.getValue());
        }

        history.entrySet().stream()
                .filter(e -> e.getValue().size() > 2)
                .forEach(e -> computeCorrelation(e.getKey(), e.getValue()));
    }

    private void computeCorrelation(String key, Collection<ResponseTimeSummary> summaries) {
        File tempFile = null;
        try {
            tempFile = CommandLineUtils.writeToTempFile(summaries,
                    s -> s.getRequestCount() + " " + s.getMeanResponseTime() + "\n", "ad_corr_");
            CommandOutput output = CommandLineUtils.runCommand(rDirectory, "Rscript",
                    "correlation.R", tempFile.getAbsolutePath());
            if (output.getStatus() != 0) {
                throw new IOException(output.toString());
            }
            String line = output.getStdout();
            log.info("Correlation analysis output: {}", line);
            String[] segments = line.split(" ");
            double correlation = Double.parseDouble(segments[0]);
            double dtw = Double.parseDouble(segments[1]);
            double lastDtw = prevDtw.getOrDefault(key, -1.0);
            if (correlation < correlationThreshold && lastDtw > 0) {
                // If the correlation has dropped and the DTW distance has increased, we
                // might be looking at a performance anomaly.
                double dtwIncrease = (dtw - lastDtw)*100.0/lastDtw;
                if (dtwIncrease > dtwIncreasePercentageThreshold) {
                    log.warn("Anomaly detected -- correlation: {}; dtwIncrease: {}%",
                            correlation, dtwIncrease);
                }
            }
            prevDtw.put(key, dtw);
        } catch (IOException | InterruptedException e) {
            log.error("Error computing the correlation statistics", e);
        } finally {
            FileUtils.deleteQuietly(tempFile);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends AnomalyDetectorBuilder<CorrelationBasedDetector,Builder> {

        private int historyLength = 60;
        private String rDirectory = "r";
        private double correlationThreshold = 0.5;
        private double dtwIncreasePercentageThreshold = 20.0;

        private Builder() {
        }

        @Override
        protected Builder getThisObj() {
            return this;
        }

        public Builder setHistoryLength(int historyLength) {
            this.historyLength = historyLength;
            return this;
        }

        public Builder setrDirectory(String rDirectory) {
            this.rDirectory = rDirectory;
            return this;
        }

        public Builder setCorrelationThreshold(double correlationThreshold) {
            this.correlationThreshold = correlationThreshold;
            return this;
        }

        public Builder setDtwIncreasePercentageThreshold(double dtwIncreasePercentageThreshold) {
            this.dtwIncreasePercentageThreshold = dtwIncreasePercentageThreshold;
            return this;
        }

        @Override
        public CorrelationBasedDetector build() {
            return new CorrelationBasedDetector(this);
        }
    }
}
