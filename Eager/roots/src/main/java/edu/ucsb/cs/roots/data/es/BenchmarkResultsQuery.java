package edu.ucsb.cs.roots.data.es;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;

public final class BenchmarkResultsQuery extends Query {

    private static final String BENCHMARK_RESULTS_QUERY = Query.loadTemplate(
            "benchmark_results_query.json");

    private final String benchmarkTimestampField;
    private final long start;
    private final long end;

    private BenchmarkResultsQuery(Builder builder) {
        checkArgument(!Strings.isNullOrEmpty(builder.benchmarkTimestampField));
        checkArgument(builder.start <= builder.end);
        this.benchmarkTimestampField = builder.benchmarkTimestampField;
        this.start = builder.start;
        this.end = builder.end;
    }

    @Override
    public String getJsonString() {
        return String.format(BENCHMARK_RESULTS_QUERY, benchmarkTimestampField, start, end,
                benchmarkTimestampField);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private String benchmarkTimestampField;
        private long start;
        private long end;

        private Builder() {
        }

        public Builder setBenchmarkTimestampField(String benchmarkTimestampField) {
            this.benchmarkTimestampField = benchmarkTimestampField;
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
            return new BenchmarkResultsQuery(this).getJsonString();
        }
    }
}
