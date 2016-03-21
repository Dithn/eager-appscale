package edu.ucsb.cs.roots.anomaly;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import edu.ucsb.cs.roots.data.ResponseTimeSummary;
import edu.ucsb.cs.roots.utils.CommandLineUtils;
import edu.ucsb.cs.roots.utils.CommandOutput;
import edu.ucsb.cs.roots.utils.ImmutableCollectors;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class CorrelationBasedDetector extends AnomalyDetector {

    private final int historyLengthInSeconds;
    private final Map<String,List<ResponseTimeSummary>> history;
    private final File scriptDirectory;
    private final double correlationThreshold;
    private final double dtwIncreaseThreshold;

    private long end = -1L;
    private Map<String,Double> prevDtw = new HashMap<>();

    private CorrelationBasedDetector(Builder builder) {
        super(builder.application, builder.periodInSeconds, builder.dataStore);
        checkArgument(builder.historyLengthInSeconds > 0, "History length must be positive");
        checkNotNull(builder.scriptDirectory, "Script directory path must not be null");
        checkArgument(builder.correlationThreshold >= -1 && builder.correlationThreshold <= 1,
                "Correlation threshold must be in the interval [-1,1]");
        checkArgument(builder.dtwIncreaseThreshold > 0,
                "DTW increase percentage threshold must be positive");
        this.historyLengthInSeconds = builder.historyLengthInSeconds;
        this.history = new HashMap<>();
        this.scriptDirectory = new File(builder.scriptDirectory);
        this.correlationThreshold = builder.correlationThreshold;
        this.dtwIncreaseThreshold = builder.dtwIncreaseThreshold;
        checkArgument(scriptDirectory.exists(), "Script directory path does not exist: %s",
                scriptDirectory.getAbsolutePath());
        checkArgument(scriptDirectory.isDirectory(), "%s is not a directory",
                scriptDirectory.getAbsolutePath());
    }

    @Override
    public void run() {
        long start;
        if (end < 0) {
            end = System.currentTimeMillis() - 60 * 1000;
            start = end - periodInSeconds * 1000;
        } else {
            start = end;
            end += periodInSeconds * 1000;
        }

        ImmutableMap<String,ResponseTimeSummary> summaries = dataStore.getResponseTimeSummary(
                application, start, end);
        for (Map.Entry<String,ResponseTimeSummary> entry : summaries.entrySet()) {
            List<ResponseTimeSummary> record = history.get(entry.getKey());
            if (record == null) {
                record = new ArrayList<>();
                history.put(entry.getKey(), record);
            }
            record.add(entry.getValue());
        }

        long cutoff = end - historyLengthInSeconds * 1000;
        history.values().forEach(v -> cleanupOldData(cutoff, v));
        history.entrySet().stream()
                .filter(e -> summaries.containsKey(e.getKey()) && e.getValue().size() > 2)
                .forEach(e -> computeCorrelation(e.getKey(), e.getValue()));
    }

    private void cleanupOldData(long cutoff, List<ResponseTimeSummary> summaries) {
        ImmutableList<ResponseTimeSummary> oldData = summaries.stream()
                .filter(s -> s.getTimestamp() < cutoff)
                .collect(ImmutableCollectors.toList());
        oldData.forEach(summaries::remove);
    }

    private void computeCorrelation(String key, Collection<ResponseTimeSummary> summaries) {
        File tempFile = null;
        try {
            tempFile = CommandLineUtils.writeToTempFile(summaries,
                    s -> s.getRequestCount() + " " + s.getMeanResponseTime() + "\n", "ad_corr_");
            CommandOutput output = CommandLineUtils.runCommand(scriptDirectory, "Rscript",
                    "correlation.R", tempFile.getAbsolutePath());
            if (output.getStatus() != 0) {
                throw new IOException(output.toString());
            }
            String line = output.getStdout();
            log.info("Correlation analysis output [{}]: {}", key, line);
            String[] segments = line.split(" ");
            double correlation = Double.parseDouble(segments[0]);
            double dtw = Double.parseDouble(segments[1]);
            double lastDtw = prevDtw.getOrDefault(key, -1.0);
            if (correlation < correlationThreshold && lastDtw > 0) {
                // If the correlation has dropped and the DTW distance has increased, we
                // might be looking at a performance anomaly.
                double dtwIncrease = (dtw - lastDtw)*100.0/lastDtw;
                if (dtwIncrease > dtwIncreaseThreshold) {
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

        private int historyLengthInSeconds = 60 * 60;
        private String scriptDirectory = "r";
        private double correlationThreshold = 0.5;
        private double dtwIncreaseThreshold = 20.0;

        private Builder() {
        }

        public Builder setHistoryLengthInSeconds(int historyLengthInSeconds) {
            this.historyLengthInSeconds = historyLengthInSeconds;
            return this;
        }

        public Builder setScriptDirectory(String scriptDirectory) {
            this.scriptDirectory = scriptDirectory;
            return this;
        }

        public Builder setCorrelationThreshold(double correlationThreshold) {
            this.correlationThreshold = correlationThreshold;
            return this;
        }

        public Builder setDtwIncreaseThreshold(double dtwIncreaseThreshold) {
            this.dtwIncreaseThreshold = dtwIncreaseThreshold;
            return this;
        }

        public CorrelationBasedDetector build() {
            return new CorrelationBasedDetector(this);
        }

        @Override
        protected Builder getThisObj() {
            return this;
        }
    }

}
