package edu.ucsb.cs.roots.bi;

import com.google.common.base.Strings;

import java.util.Date;

import static com.google.common.base.Preconditions.checkArgument;

public final class Bottleneck {

    private final String apiCall;
    private final Date onsetTime;

    public Bottleneck(String apiCall, Date onsetTime) {
        checkArgument(!Strings.isNullOrEmpty(apiCall), "API call is required");
        this.apiCall = apiCall;
        this.onsetTime = onsetTime;
    }

    public Bottleneck(String apiCall) {
        this(apiCall, null);
    }

    public String getApiCall() {
        return apiCall;
    }

    public Date getOnsetTime() {
        return onsetTime;
    }

    @Override
    public String toString() {
        return String.format("apiCall: %s, onsetTime: %s", apiCall,
                onsetTime != null ? onsetTime.toString() : "Unknown");
    }
}
