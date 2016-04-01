package edu.ucsb.cs.roots.data.es;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;

public class RequestInfoQuery extends Query {

    private static final String REQUEST_INFO_QUERY = Query.loadTemplate(
            "request_info_query.json");

    private final String apiCallTimestampField;
    private final String applicationField;
    private final String application;
    private final long start;
    private final long end;

    private RequestInfoQuery(Builder builder) {
        checkArgument(!Strings.isNullOrEmpty(builder.apiCallTimestampField));
        checkArgument(!Strings.isNullOrEmpty(builder.applicationField));
        checkArgument(!Strings.isNullOrEmpty(builder.application));
        checkArgument(builder.start <= builder.end);
        this.apiCallTimestampField = builder.apiCallTimestampField;
        this.applicationField = builder.applicationField;
        this.application = builder.application;
        this.start = builder.start;
        this.end = builder.end;
    }

    @Override
    public String getJsonString() {
        return String.format(REQUEST_INFO_QUERY, applicationField, application,
                apiCallTimestampField, start, end);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private String apiCallTimestampField;
        private String applicationField;
        private String application;
        private long start;
        private long end;

        private Builder() {
        }

        public Builder setApiCallTimestampField(String apiCallTimestampField) {
            this.apiCallTimestampField = apiCallTimestampField;
            return this;
        }

        public Builder setApplicationField(String applicationField) {
            this.applicationField = applicationField;
            return this;
        }

        public Builder setApplication(String application) {
            this.application = application;
            return this;
        }

        public Builder setStart(long start) {
            this.start = start;
            return this;
        }

        public Builder setEnd(long end) {
            this.end = end;
            return this;
        }

        public String buildJsonString() {
            return new RequestInfoQuery(this).getJsonString();
        }
    }
}
