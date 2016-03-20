package edu.ucsb.cs.roots.data;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class TestDataStore extends DataStore {

    @Override
    public Map<String, ResponseTimeSummary> getResponseTimeSummary(
            String application, long start, long end) {
        List<AccessLogEntry> logEntries = getAccessLogEntries(application, start, end);
        Map<String,List<AccessLogEntry>> groupedEntries = logEntries.stream()
                .collect(Collectors.groupingBy(AccessLogEntry::getRequestType));
        return groupedEntries.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new ResponseTimeSummary(e.getValue())));
    }

    private List<AccessLogEntry> getAccessLogEntries(String application, long start, long end) {
        Random rand = new Random();
        int recordCount = rand.nextInt(100);
        ImmutableList.Builder<AccessLogEntry> builder = ImmutableList.builder();
        for (int i = 0; i < recordCount; i++) {
            long offset = rand.nextInt((int) (end - start));
            String method;
            if (rand.nextBoolean()) {
                method = "GET";
            } else {
                method = "POST";
            }
            AccessLogEntry record = new AccessLogEntry(start + offset, application,
                    method, "/", rand.nextInt(50));
            builder.add(record);
        }
        return builder.build();
    }
}
