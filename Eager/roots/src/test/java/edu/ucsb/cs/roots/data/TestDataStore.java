package edu.ucsb.cs.roots.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import edu.ucsb.cs.roots.RootsEnvironment;

import java.util.ArrayList;
import java.util.List;

public class TestDataStore implements DataStore {

    public static final String GET_BENCHMARK_RESULTS = "GET_BENCHMARK_RESULTS";

    private final List<DataStoreCall> calls = new ArrayList<>();

    public TestDataStore(RootsEnvironment environment, String name) {
        environment.getDataStoreService().put(name, this);
    }

    @Override
    public ImmutableListMultimap<String,AccessLogEntry> getBenchmarkResults(
            String application, long start, long end) throws DataStoreException {
        calls.add(new DataStoreCall(start, end, GET_BENCHMARK_RESULTS, application));
        return ImmutableListMultimap.of();
    }

    public int callCount() {
        return calls.size();
    }

    public List<DataStoreCall> getCallsAndClear() {
        ImmutableList<DataStoreCall> copy = ImmutableList.copyOf(calls);
        calls.clear();
        return copy;
    }

}
