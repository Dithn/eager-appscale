package edu.ucsb.cs.roots.data.es;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;

public final class ResponseTimeSummaryQuery extends Query {

    private static final String RESPONSE_TIME_SUMMARY_QUERY = Query.loadTemplate(
            "response_time_summary_query.json");

    private final long start;
    private final long end;
    private final String accessLogTimestampField;
    private final String accessLogMethodField;
    private final String accessLogPathField;
    private final String accessLogResponseTimeField;

    private ResponseTimeSummaryQuery(Builder builder) {
        checkArgument(builder.start <= builder.end);
        checkArgument(!Strings.isNullOrEmpty(builder.accessLogTimestampField));
        checkArgument(!Strings.isNullOrEmpty(builder.accessLogMethodField));
        checkArgument(!Strings.isNullOrEmpty(builder.accessLogPathField));
        checkArgument(!Strings.isNullOrEmpty(builder.accessLogResponseTimeField));
        this.start = builder.start;
        this.end = builder.end;
        this.accessLogTimestampField = builder.accessLogTimestampField;
        this.accessLogMethodField = builder.accessLogMethodField;
        this.accessLogPathField = builder.accessLogPathField;
        this.accessLogResponseTimeField = builder.accessLogResponseTimeField;
    }

    @Override
    public String getJsonString() {
        return String.format(RESPONSE_TIME_SUMMARY_QUERY,
                accessLogTimestampField, start, end, accessLogMethodField, accessLogPathField,
                accessLogResponseTimeField);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String accessLogTimestampField;
        private long start;
        private long end;
        private String accessLogMethodField;
        private String accessLogPathField;
        private String accessLogResponseTimeField;

        private Builder() {
        }

        public Builder setAccessLogTimestampField(String accessLogTimestampField) {
            this.accessLogTimestampField = accessLogTimestampField;
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

        public Builder setAccessLogMethodField(String accessLogMethodField) {
            this.accessLogMethodField = accessLogMethodField;
            return this;
        }

        public Builder setAccessLogPathField(String accessLogPathField) {
            this.accessLogPathField = accessLogPathField;
            return this;
        }

        public Builder setAccessLogResponseTimeField(String accessLogResponseTimeField) {
            this.accessLogResponseTimeField = accessLogResponseTimeField;
            return this;
        }

        public String buildJsonString() {
            return new ResponseTimeSummaryQuery(this).getJsonString();
        }
    }
}
