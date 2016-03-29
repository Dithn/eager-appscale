package edu.ucsb.cs.roots.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import edu.ucsb.cs.roots.utils.ImmutableCollectors;

import java.util.*;
import java.util.stream.Collectors;

public class RandomDataStore implements DataStore {

    private static final Random RAND = new Random();

    @Override
    public ImmutableMap<String, ResponseTimeSummary> getResponseTimeSummary(
            String application, long start, long end) {
        List<AccessLogEntry> logEntries = getAccessLogEntries(application, start, end);
        Map<String,List<AccessLogEntry>> groupedEntries = logEntries.stream()
                .collect(Collectors.groupingBy(AccessLogEntry::getRequestType));
        return groupedEntries.entrySet().stream().collect(ImmutableCollectors.toMap(
                        Map.Entry::getKey, e -> new ResponseTimeSummary(start, e.getValue())));
    }

    @Override
    public ImmutableList<Double> getWorkloadSummary(
            String application, String operation, long start, long end,
            long period) throws DataStoreException {
        ImmutableList.Builder<Double> builder = ImmutableList.builder();
        double changePoint = start + (end - start) * 0.7;
        boolean injectChange = RAND.nextBoolean();
        for (long i = start; i < end; i += period) {
            Double element = (double) RAND.nextInt(10);
            if (injectChange && i >= changePoint) {
                element += 20;
            }
            builder.add(element);
        }
        return builder.build();
    }

    @Override
    public ImmutableListMultimap<String,ResponseTimeSummary> getResponseTimeHistory(
            String application, long start, long end, long period) {
        ImmutableMap<String,ResponseTimeSummary> lastPeriod = getResponseTimeSummary(
                application, end - period, end);
        ImmutableListMultimap.Builder<String,ResponseTimeSummary> builder = ImmutableListMultimap.builder();
        lastPeriod.forEach(builder::put);
        return builder.build();
    }

    @Override
    public ImmutableListMultimap<String,AccessLogEntry> getBenchmarkResults(
            String application, long start, long end) {
        List<AccessLogEntry> logEntries = getAccessLogEntries(application, start, end, 2);
        Map<String,List<AccessLogEntry>> groupedEntries = logEntries.stream()
                .collect(Collectors.groupingBy(AccessLogEntry::getRequestType));
        ImmutableListMultimap.Builder<String,AccessLogEntry> builder = ImmutableListMultimap.builder();
        groupedEntries.forEach(builder::putAll);
        return builder.build();
    }

    @Override
    public ImmutableListMultimap<String,ApplicationRequest> getRequestInfo(
            String application, long start, long end) {
        ImmutableListMultimap.Builder<String,ApplicationRequest> builder = ImmutableListMultimap.builder();
        builder.putAll("GET /", getApplicationRequests(
                application, "GET /", start, end, RAND.nextInt(50), 3));
        builder.putAll("POST /", getApplicationRequests(
                application, "POST /", start, end, RAND.nextInt(50), 2));
        return builder.build();
    }

    private ImmutableList<ApplicationRequest> getApplicationRequests(
            String application, String operation, long start, long end, int recordCount,
            int apiCalls) {
        ImmutableList.Builder<ApplicationRequest> builder = ImmutableList.builder();
        for (int i = 0; i < recordCount; i++) {
            long offset = RAND.nextInt((int) (end - start));
            ImmutableList.Builder<ApiCall> callBuilder = ImmutableList.builder();
            for (int j = 0; j < apiCalls; j++) {
                callBuilder.add(new ApiCall(start + offset, "datastore", "op" + j, RAND.nextInt(30)));
            }
            ApplicationRequest record = new ApplicationRequest(start + offset, application,
                    operation, callBuilder.build());
            builder.add(record);
        }
        return builder.build();
    }

    private List<AccessLogEntry> getAccessLogEntries(
            String application, long start, long end) {
        return getAccessLogEntries(application, start, end, RAND.nextInt(100));
    }

    private ImmutableList<AccessLogEntry> getAccessLogEntries(
            String application, long start, long end, int recordCount) {
        ImmutableList.Builder<AccessLogEntry> builder = ImmutableList.builder();
        for (int i = 0; i < recordCount; i++) {
            long offset = RAND.nextInt((int) (end - start));
            String method;
            if (i % 2 == 0) {
                method = "GET";
            } else {
                method = "POST";
            }
            AccessLogEntry record = new AccessLogEntry(start + offset, application,
                    method, "/", RAND.nextInt(50));
            builder.add(record);
        }
        return builder.build();
    }
}
