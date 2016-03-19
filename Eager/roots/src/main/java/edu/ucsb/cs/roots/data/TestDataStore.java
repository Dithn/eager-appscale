package edu.ucsb.cs.roots.data;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Random;

public class TestDataStore extends DataStore {

    @Override
    public List<AccessLogEntry> getAccessLogEntries(String application, long start, long end) {
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
