package edu.ucsb.cs.roots.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestDataStore extends DataStore {

    @Override
    public AccessLogEntry[] getAccessLogEntries(String application, long start, long end) {
        Random rand = new Random();
        int recordCount = rand.nextInt(100);
        List<AccessLogEntry> records = new ArrayList<>();
        for (int i = 0; i < recordCount; i++) {
            long offset = rand.nextInt((int) (end - start));
            AccessLogEntry record = new AccessLogEntry(start + offset, application,
                    "GET", "/", rand.nextInt(50));
            records.add(record);
        }
        return records.toArray(new AccessLogEntry[records.size()]);
    }
}
