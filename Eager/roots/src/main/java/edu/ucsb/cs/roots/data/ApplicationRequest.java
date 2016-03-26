package edu.ucsb.cs.roots.data;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class ApplicationRequest {

    private final long timestamp;
    private final String application;
    private final String operation;
    private final ImmutableList<ApiCall> apiCalls;

    public ApplicationRequest(long timestamp, String application, String operation,
                              ImmutableList<ApiCall> apiCalls) {
        checkArgument(timestamp > 0, "Timestamp must be positive");
        checkArgument(!Strings.isNullOrEmpty(application), "Application is required");
        checkArgument(!Strings.isNullOrEmpty(operation), "Operation is required");
        checkNotNull(apiCalls, "ApiCall list must not be null");
        this.timestamp = timestamp;
        this.application = application;
        this.operation = operation;
        this.apiCalls = apiCalls;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getApplication() {
        return application;
    }

    public String getOperation() {
        return operation;
    }

    public ImmutableList<ApiCall> getApiCalls() {
        return apiCalls;
    }

    public String getPathAsString() {
        Optional<String> path = apiCalls.stream().map(ApiCall::name).reduce((a, b) -> a + ", " + b);
        if (path.isPresent()) {
            return path.get();
        } else {
            return "";
        }
    }

    public int getTotalTime() {
        return apiCalls.stream().mapToInt(ApiCall::getTimeElapsed).sum();
    }
}
