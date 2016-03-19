package edu.ucsb.cs.roots.anomaly;

import edu.ucsb.cs.roots.data.AccessLogEntry;
import edu.ucsb.cs.roots.utils.CommandLineUtils;
import edu.ucsb.cs.roots.utils.CommandOutput;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Based on "Detection of Performance Anomalies in Web-based Applications" by Magalhaes and Silva.
 * Calculates the correlation between response time and number of requests to detect any
 * anomalous increases in response time.
 */
public class CorrelationBasedAnomalyDetector extends AnomalyDetector {

    private final int historyLength;
    private final Map<String,List<Summary>> history;
    private final File rDirectory;

    private long end = -1L;

    private CorrelationBasedAnomalyDetector(Builder builder) {
        super(builder.application, builder.period, builder.timeUnit, builder.dataStore);
        checkArgument(builder.historyLength > 10, "History length must be greater than 10");
        this.historyLength = builder.historyLength;
        this.history = new HashMap<>();
        this.rDirectory = new File(builder.rDirectory);
        checkArgument(rDirectory.exists() && rDirectory.isDirectory(),
                "R directory does not exist or is not a directory: " + rDirectory.getAbsolutePath());
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

        List<AccessLogEntry> logEntries = dataStore.getAccessLogEntries(application, start, end);
        Map<String,List<AccessLogEntry>> groupedEntries = logEntries.stream()
                .collect(Collectors.groupingBy(AccessLogEntry::getRequestType));
        Map<String,Summary> summaries = groupedEntries.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new Summary(e.getValue())));

        for (Map.Entry<String,Summary> entry : summaries.entrySet()) {
            List<Summary> record = history.get(entry.getKey());
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

    private void computeCorrelation(String key, List<Summary> summaries) {
        StringBuilder sb = new StringBuilder();
        summaries.stream().forEach(h ->
                sb.append(h.requestCount).append(" ").append(h.responseTime).append('\n'));
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

    private static class Summary {
        private final double responseTime;
        private final double requestCount;

        private Summary(Collection<AccessLogEntry> entries) {
            if (entries.size() > 0) {
                responseTime = entries.stream()
                        .mapToDouble(AccessLogEntry::getResponseTime)
                        .average()
                        .getAsDouble();
            } else {
                responseTime = 0.0;
            }
            requestCount = entries.size();
        }
    }

    public static class Builder extends AnomalyDetectorBuilder<CorrelationBasedAnomalyDetector,Builder> {

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
        public CorrelationBasedAnomalyDetector build() {
            return new CorrelationBasedAnomalyDetector(this);
        }
    }
}
