package edu.ucsb.cs.roots.data;

import java.util.Map;

public abstract class DataStore {

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
    public abstract Map<String,ResponseTimeSummary> getResponseTimeSummary(
            String application, long start, long end);

}
