package edu.ucsb.cs.roots.data.es;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import junit.framework.Assert;
import org.junit.Test;

public class QueryTest {

    @Test
    public void testResponseTimeSummaryQuery() {
        String string = ResponseTimeSummaryQuery.newBuilder()
                .setStart(0)
                .setEnd(100)
                .setAccessLogTimestampField("timestamp")
                .setAccessLogMethodField("method")
                .setAccessLogPathField("path")
                .setAccessLogResponseTimeField("responseTime")
                .buildJsonString();
        JsonObject element = parseString(string);
        Assert.assertEquals(0, element.get("size").getAsInt());

        JsonObject timeRange = element.getAsJsonObject("query")
                .getAsJsonObject("bool").getAsJsonObject("filter")
                .getAsJsonObject("range").getAsJsonObject("timestamp");
        Assert.assertEquals(0, timeRange.get("gte").getAsLong());
        Assert.assertEquals(100, timeRange.get("lt").getAsLong());

        String method = element.getAsJsonObject("aggs").getAsJsonObject("methods")
                .getAsJsonObject("terms").get("field").getAsString();
        Assert.assertEquals("method", method);

        String path = element.getAsJsonObject("aggs").getAsJsonObject("methods")
                .getAsJsonObject("aggs").getAsJsonObject("paths")
                .getAsJsonObject("terms").get("field").getAsString();
        Assert.assertEquals("path", path);

        String responseTime = element.getAsJsonObject("aggs").getAsJsonObject("methods")
                .getAsJsonObject("aggs").getAsJsonObject("paths")
                .getAsJsonObject("aggs").getAsJsonObject("avg_time")
                .getAsJsonObject("avg").get("field").getAsString();
        Assert.assertEquals("responseTime", responseTime);
    }

    @Test
    public void testResponseTimeHistoryQuery() {
        String string = ResponseTimeHistoryQuery.newBuilder()
                .setStart(0)
                .setEnd(100)
                .setPeriod(10)
                .setAccessLogTimestampField("timestamp")
                .setAccessLogMethodField("method")
                .setAccessLogPathField("path")
                .setAccessLogResponseTimeField("responseTime")
                .buildJsonString();
        JsonObject element = parseString(string);
        Assert.assertEquals(0, element.get("size").getAsInt());

        JsonObject timeRange = element.getAsJsonObject("query")
                .getAsJsonObject("bool").getAsJsonObject("filter")
                .getAsJsonObject("range").getAsJsonObject("timestamp");
        Assert.assertEquals(0, timeRange.get("gte").getAsLong());
        Assert.assertEquals(100, timeRange.get("lt").getAsLong());

        String method = element.getAsJsonObject("aggs").getAsJsonObject("methods")
                .getAsJsonObject("terms").get("field").getAsString();
        Assert.assertEquals("method", method);

        String path = element.getAsJsonObject("aggs").getAsJsonObject("methods")
                .getAsJsonObject("aggs").getAsJsonObject("paths")
                .getAsJsonObject("terms").get("field").getAsString();
        Assert.assertEquals("path", path);

        JsonObject histogram = element.getAsJsonObject("aggs").getAsJsonObject("methods")
                .getAsJsonObject("aggs").getAsJsonObject("paths")
                .getAsJsonObject("aggs").getAsJsonObject("periods")
                .getAsJsonObject("histogram");
        Assert.assertEquals("timestamp", histogram.get("field").getAsString());
        Assert.assertEquals(10, histogram.get("interval").getAsLong());
        Assert.assertEquals(0, histogram.get("offset").getAsLong());
        Assert.assertEquals(0, histogram.getAsJsonObject("extended_bounds").get("min").getAsLong());
        Assert.assertEquals(100 - 10, histogram.getAsJsonObject("extended_bounds").get("max").getAsLong());

        String responseTime = element.getAsJsonObject("aggs").getAsJsonObject("methods")
                .getAsJsonObject("aggs").getAsJsonObject("paths")
                .getAsJsonObject("aggs").getAsJsonObject("periods")
                .getAsJsonObject("aggs").getAsJsonObject("avg_time")
                .getAsJsonObject("avg").get("field").getAsString();
        Assert.assertEquals("responseTime", responseTime);
    }

    @Test
    public void testBenchmarkResultsQuery() {
        String string = BenchmarkResultsQuery.newBuilder()
                .setStart(0)
                .setEnd(100)
                .setBenchmarkTimestampField("timestamp")
                .buildJsonString();
        JsonObject element = parseString(string);
        Assert.assertEquals(4000, element.get("size").getAsInt());

        JsonObject timeRange = element.getAsJsonObject("query")
                .getAsJsonObject("bool").getAsJsonObject("filter")
                .getAsJsonObject("range").getAsJsonObject("timestamp");
        Assert.assertEquals(0, timeRange.get("gte").getAsLong());
        Assert.assertEquals(100, timeRange.get("lt").getAsLong());

        Assert.assertEquals("asc", element.getAsJsonObject("sort").get("timestamp").getAsString());
    }

    @Test
    public void testWorkloadSummaryQuery() {
        String string = WorkloadSummaryQuery.newBuilder()
                .setStart(0)
                .setEnd(100)
                .setPeriod(10)
                .setAccessLogTimestampField("timestamp")
                .setAccessLogMethodField("method")
                .setAccessLogPathField("path")
                .setMethod("GET")
                .setPath("/benchmark")
                .buildJsonString();
        JsonObject element = parseString(string);
        Assert.assertEquals(0, element.get("size").getAsInt());

        JsonObject timeRange = element.getAsJsonObject("query")
                .getAsJsonObject("bool").getAsJsonObject("filter")
                .getAsJsonObject("range").getAsJsonObject("timestamp");
        Assert.assertEquals(0, timeRange.get("gte").getAsLong());
        Assert.assertEquals(100, timeRange.get("lt").getAsLong());

        JsonArray termFilters = element.getAsJsonObject("query")
                .getAsJsonObject("bool").getAsJsonArray("must");
        Assert.assertEquals(2, termFilters.size());
        String method = termFilters.get(0).getAsJsonObject().getAsJsonObject("term")
                .get("method").getAsString();
        Assert.assertEquals("GET", method);
        String path = termFilters.get(1).getAsJsonObject().getAsJsonObject("term")
                .get("path").getAsString();
        Assert.assertEquals("/benchmark", path);

        JsonObject histogram = element.getAsJsonObject("aggs").getAsJsonObject("periods")
                .getAsJsonObject("histogram");
        Assert.assertEquals("timestamp", histogram.get("field").getAsString());
        Assert.assertEquals(10, histogram.get("interval").getAsLong());
        Assert.assertEquals(0, histogram.get("offset").getAsLong());
        Assert.assertEquals(0, histogram.getAsJsonObject("extended_bounds").get("min").getAsLong());
        Assert.assertEquals(100 - 10, histogram.getAsJsonObject("extended_bounds").get("max").getAsLong());
    }

    @Test
    public void testScrollQuery() {
        String string = ScrollQuery.build("test-scroll-id");
        JsonObject element = parseString(string);
        Assert.assertEquals("test-scroll-id", element.get("scroll_id").getAsString());
        Assert.assertEquals("1m", element.get("scroll").getAsString());
    }

    @Test
    public void testRequestInfoQuery() {
        String string = RequestInfoQuery.newBuilder()
                .setStart(0)
                .setEnd(100)
                .setApiCallRequestTimestampField("requestTimestamp")
                .setApiCallSequenceNumberField("sequenceNumber")
                .buildJsonString();
        JsonObject element = parseString(string);
        Assert.assertEquals(4000, element.get("size").getAsInt());

        JsonObject timeRange = element.getAsJsonObject("query")
                .getAsJsonObject("bool").getAsJsonObject("filter")
                .getAsJsonObject("range").getAsJsonObject("requestTimestamp");
        Assert.assertEquals(0, timeRange.get("gte").getAsLong());
        Assert.assertEquals(100, timeRange.get("lt").getAsLong());

        JsonArray sort = element.getAsJsonArray("sort");
        Assert.assertEquals("asc", sort.get(0).getAsJsonObject().get("requestTimestamp")
                .getAsString());
        Assert.assertEquals("asc", sort.get(1).getAsJsonObject().get("sequenceNumber")
                .getAsString());
    }

    @Test
    public void testRequestInfoByOperationQuery() {
        String string = RequestInfoByOperationQuery.newBuilder()
                .setStart(0)
                .setEnd(100)
                .setApiCallRequestTimestampField("requestTimestamp")
                .setApiCallSequenceNumberField("sequenceNumber")
                .setRequestOperationField("requestOperation.raw")
                .setRequestOperation("GET /")
                .buildJsonString();
        JsonObject element = parseString(string);
        Assert.assertEquals(4000, element.get("size").getAsInt());

        String termFilter = element.getAsJsonObject("query")
                .getAsJsonObject("bool").getAsJsonObject("must")
                .getAsJsonObject("term").get("requestOperation.raw").getAsString();
        Assert.assertEquals("GET /", termFilter);

        JsonObject timeRange = element.getAsJsonObject("query")
                .getAsJsonObject("bool").getAsJsonObject("filter")
                .getAsJsonObject("range").getAsJsonObject("requestTimestamp");
        Assert.assertEquals(0, timeRange.get("gte").getAsLong());
        Assert.assertEquals(100, timeRange.get("lt").getAsLong());

        JsonArray sort = element.getAsJsonArray("sort");
        Assert.assertEquals("asc", sort.get(0).getAsJsonObject().get("requestTimestamp")
                .getAsString());
        Assert.assertEquals("asc", sort.get(1).getAsJsonObject().get("sequenceNumber")
                .getAsString());
    }

    private JsonObject parseString(String s) {
        JsonParser parser = new JsonParser();
        return parser.parse(s).getAsJsonObject();
    }

}
