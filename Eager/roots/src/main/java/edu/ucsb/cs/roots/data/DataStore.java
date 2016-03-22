package edu.ucsb.cs.roots.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DataStore {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public void init() {
    }

    public void destroy() {
    }

    /**
     * Retrieve the response time statistics for the specified application by analyzing
     * the request traffic within the specified interval. Returns a map of request types
     * and response time data corresponding to each request type.
     *
     * @param application Name of the application
     * @param start Start time of the interval (inclusive)
     * @param end End time of the interval (exclusive)
     * @return A Map of request types (String) and response time data (ResponseTimeSummary)
     */
    public abstract ImmutableMap<String,ResponseTimeSummary> getResponseTimeSummary(
            String application, long start, long end) throws DataStoreException;

    public abstract ImmutableMap<String,ImmutableList<ResponseTimeSummary>> getResponseTimeHistory(
            String application, long start, long end, long period) throws DataStoreException;

    /**
     * Retrieve the HTTP API benchmark results for the specified application by analyzing the
     * data gathered during the specified interval. Returns a map of request types and benchmarking
     * results for each request type.
     *
     * @param application Name of the application
     * @param start Start time of the interval (inclusive)
     * @param end End time of the interval (exclusive)
     * @return A Map of request types (String) and benchmark results for each type
     */
    public abstract ImmutableMap<String,ImmutableList<AccessLogEntry>> getBenchmarkResults(
            String application, long start, long end) throws DataStoreException;

}
