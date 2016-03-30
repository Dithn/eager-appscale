package edu.ucsb.cs.roots.data.es;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;

public class ResponseTimeHistoryQuery extends Query {

    private static final String RESPONSE_TIME_HISTORY_QUERY = Query.loadTemplate(
            "response_time_history_query.json");

    private final long start;
    private final long end;
    private final long period;
    private final String accessLogTimestampField;
    private final String accessLogMethodField;
    private final String accessLogPathField;
    private final String accessLogResponseTimeField;

    private ResponseTimeHistoryQuery(Builder builder) {
        checkArgument(builder.start <= builder.end);
        checkArgument(builder.period > 0);
        checkArgument(!Strings.isNullOrEmpty(builder.accessLogTimestampField));
        checkArgument(!Strings.isNullOrEmpty(builder.accessLogMethodField));
        checkArgument(!Strings.isNullOrEmpty(builder.accessLogPathField));
        checkArgument(!Strings.isNullOrEmpty(builder.accessLogResponseTimeField));
        this.start = builder.start;
        this.end = builder.end;
        this.period = builder.period;
        this.accessLogTimestampField = builder.accessLogTimestampField;
        this.accessLogMethodField = builder.accessLogMethodField;
        this.accessLogPathField = builder.accessLogPathField;
        this.accessLogResponseTimeField = builder.accessLogResponseTimeField;
    }

    @Override
    public String getJsonString() {
        return String.format(RESPONSE_TIME_HISTORY_QUERY,
                accessLogTimestampField, start, end, accessLogMethodField, accessLogPathField,
                accessLogTimestampField, period, start, end - period, accessLogResponseTimeField);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private long start;
        private long end;
        private long period;
        private String accessLogTimestampField;
        private String accessLogMethodField;
        private String accessLogPathField;
        private String accessLogResponseTimeField;

        private Builder() {
        }

        public Builder setStart(long start) {
            this.start = start;
            return this;
        }

        public Builder setEnd(long end) {
            this.end = end;
            return this;
        }

        public Builder setPeriod(long period) {
            this.period = period;
            return this;
        }

        public Builder setAccessLogTimestampField(String accessLogTimestampField) {
            this.accessLogTimestampField = accessLogTimestampField;
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
            return new ResponseTimeHistoryQuery(this).getJsonString();
        }
    }
}
