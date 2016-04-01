package edu.ucsb.cs.roots.data.es;

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
        Assert.assertEquals(0, histogram.getAsJsonObject("extended_bounds").get("min").getAsLong());
        Assert.assertEquals(100 - 10, histogram.getAsJsonObject("extended_bounds").get("max").getAsLong());

        String responseTime = element.getAsJsonObject("aggs").getAsJsonObject("methods")
                .getAsJsonObject("aggs").getAsJsonObject("paths")
                .getAsJsonObject("aggs").getAsJsonObject("periods")
                .getAsJsonObject("aggs").getAsJsonObject("avg_time")
                .getAsJsonObject("avg").get("field").getAsString();
        Assert.assertEquals("responseTime", responseTime);
    }

    private JsonObject parseString(String s) {
        JsonParser parser = new JsonParser();
        return parser.parse(s).getAsJsonObject();
    }

}
