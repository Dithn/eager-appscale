package edu.ucsb.cs.roots.data.es;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;

public class WorkloadSummaryQuery extends Query {

    private static final String WORKLOAD_SUMMARY_QUERY = Query.loadTemplate(
            "workload_summary_query.json");

    private final long start;
    private final long end;
    private final long period;
    private final String method;
    private final String path;
    private final String accessLogTimestampField;
    private final String accessLogMethodField;
    private final String accessLogPathField;

    private WorkloadSummaryQuery(Builder builder) {
        super(builder.rawStringFilter);
        checkArgument(builder.start <= builder.end);
        checkArgument(builder.period > 0);
        checkArgument(!Strings.isNullOrEmpty(builder.method));
        checkArgument(!Strings.isNullOrEmpty(builder.path));
        checkArgument(!Strings.isNullOrEmpty(builder.accessLogTimestampField));
        checkArgument(!Strings.isNullOrEmpty(builder.accessLogMethodField));
        checkArgument(!Strings.isNullOrEmpty(builder.accessLogPathField));
        this.start = builder.start;
        this.end = builder.end;
        this.period = builder.period;
        this.method = builder.method;
        this.path = builder.path;
        this.accessLogTimestampField = builder.accessLogTimestampField;
        this.accessLogMethodField = builder.accessLogMethodField;
        this.accessLogPathField = builder.accessLogPathField;
    }

    @Override
    public String getJsonString() {
        return String.format(WORKLOAD_SUMMARY_QUERY, stringFieldName(accessLogMethodField), method,
                stringFieldName(accessLogPathField), path, accessLogTimestampField, start, end,
                accessLogTimestampField, period, start % period, start, end - period);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String method;
        private String path;
        private long start;
        private long end;
        private long period;
        private String accessLogTimestampField;
        private String accessLogMethodField;
        private String accessLogPathField;
        private boolean rawStringFilter;

        private Builder() {
        }

        public Builder setMethod(String method) {
            this.method = method;
            return this;
        }

        public Builder setPath(String path) {
            this.path = path;
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

        public Builder setRawStringFilter(boolean rawStringFilter) {
            this.rawStringFilter = rawStringFilter;
            return this;
        }

        public String buildJsonString() {
            return new WorkloadSummaryQuery(this).getJsonString();
        }
    }
}
