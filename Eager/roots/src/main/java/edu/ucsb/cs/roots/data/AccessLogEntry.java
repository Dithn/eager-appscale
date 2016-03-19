package edu.ucsb.cs.roots.data;

public final class AccessLogEntry {

    private final long timestamp;
    private final String application;
    private final String method;
    private final String path;
    private final int responseTime;

    public AccessLogEntry(long timestamp, String application, String method,
                          String path, int responseTime) {
        this.timestamp = timestamp;
        this.application = application;
        this.method = method;
        this.path = path;
        this.responseTime = responseTime;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getApplication() {
        return application;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public int getResponseTime() {
        return responseTime;
    }
}
