package edu.ucsb.cs.roots.data.es;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;

public final class RequestInfoByOperationQuery extends Query {

    private static final String REQUEST_INFO_QUERY = Query.loadTemplate(
            "request_info_by_operation_query.json");

    private final String requestOperationField;
    private final String requestOperation;
    private final String apiCallRequestTimestampField;
    private final String apiCallSequenceNumberField;
    private final long start;
    private final long end;

    private RequestInfoByOperationQuery(Builder builder) {
        checkArgument(!Strings.isNullOrEmpty(builder.requestOperationField));
        checkArgument(!Strings.isNullOrEmpty(builder.requestOperation));
        checkArgument(!Strings.isNullOrEmpty(builder.apiCallRequestTimestampField));
        checkArgument(!Strings.isNullOrEmpty(builder.apiCallSequenceNumberField));
        checkArgument(builder.start <= builder.end);
        this.requestOperationField = builder.requestOperationField;
        this.requestOperation = builder.requestOperation;
        this.apiCallRequestTimestampField = builder.apiCallRequestTimestampField;
        this.apiCallSequenceNumberField = builder.apiCallSequenceNumberField;
        this.start = builder.start;
        this.end = builder.end;
    }

    @Override
    public String getJsonString() {
        return String.format(REQUEST_INFO_QUERY, requestOperationField, requestOperation,
                apiCallRequestTimestampField, start, end, apiCallRequestTimestampField, apiCallSequenceNumberField);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private String requestOperationField;
        private String requestOperation;
        private String apiCallRequestTimestampField;
        private String apiCallSequenceNumberField;
        private long start;
        private long end;

        private Builder() {
        }

        public Builder setRequestOperationField(String requestOperationField) {
            this.requestOperationField = requestOperationField;
            return this;
        }

        public Builder setRequestOperation(String requestOperation) {
            this.requestOperation = requestOperation;
            return this;
        }

        public Builder setApiCallRequestTimestampField(String apiCallRequestTimestampField) {
            this.apiCallRequestTimestampField = apiCallRequestTimestampField;
            return this;
        }

        public Builder setApiCallSequenceNumberField(String apiCallSequenceNumberField) {
            this.apiCallSequenceNumberField = apiCallSequenceNumberField;
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
            return new RequestInfoByOperationQuery(this).getJsonString();
        }
    }
}
