package edu.ucsb.cs.roots.anomaly;

import edu.ucsb.cs.roots.data.ResponseTimeSummary;
import edu.ucsb.cs.roots.utils.CommandLineUtils;
import edu.ucsb.cs.roots.utils.CommandOutput;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Based on "Detection of Performance Anomalies in Web-based Applications" by Magalhaes and Silva.
 * Calculates the correlation between response time and number of requests to detect any
 * anomalous increases in response time.
 */
public class CorrelationBasedDetector extends AnomalyDetector {

    private final int historyLength;
    private final Map<String,List<ResponseTimeSummary>> history;
    private final File rDirectory;

    private long end = -1L;

    private CorrelationBasedDetector(Builder builder) {
        super(builder.application, builder.period, builder.timeUnit, builder.dataStore);
        checkArgument(builder.historyLength > 10, "History length must be greater than 10");
        this.historyLength = builder.historyLength;
        this.history = new HashMap<>();
        this.rDirectory = new File(builder.rDirectory);
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
        history.keySet().stream()
                .forEach(k -> summaries.putIfAbsent(k, ResponseTimeSummary.ZERO));

        for (Map.Entry<String,ResponseTimeSummary> entry : summaries.entrySet()) {
            List<ResponseTimeSummary> record = history.get(entry.getKey());
            if (record == null) {
                record = new ArrayList<>(historyLength);
                history.put(entry.getKey(), record);
            }
            if (record.size() == historyLength) {
                record.remove(0);
            }
            record.add(entry.getValue());
        }

        history.entrySet().stream()
                .filter(e -> e.getValue().size() > 2)
                .forEach(e -> computeCorrelation(e.getKey(), e.getValue()));
    }

    private void computeCorrelation(String key, List<ResponseTimeSummary> summaries) {
        StringBuilder sb = new StringBuilder();
        summaries.stream().forEach(h ->
                sb.append(h.getRequestCount()).append(" ").append(h.getMeanResponseTime()).append('\n'));
        File tempFile = null;
        try {
            tempFile = File.createTempFile("ad_corr_", ".tmp");
            FileUtils.writeStringToFile(tempFile, sb.toString(), Charset.defaultCharset());
            CommandOutput output = CommandLineUtils.runCommand(rDirectory, "Rscript",
                    "correlation.R", tempFile.getAbsolutePath());
            if (output.getStatus() != 0) {
                throw new IOException("R script terminated with status: " + output.getStatus() +
                        "; output=" + output.getStdout() + "; error=" + output.getStderr());
            }
            String line = output.getStdout();
            System.out.println(key + " " + line);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
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

        @Override
        public CorrelationBasedDetector build() {
            return new CorrelationBasedDetector(this);
        }
    }
}
