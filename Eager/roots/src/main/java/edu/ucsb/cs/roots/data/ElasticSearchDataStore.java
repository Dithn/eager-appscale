package edu.ucsb.cs.roots.data;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import edu.ucsb.cs.roots.data.es.BenchmarkResultsQuery;
import edu.ucsb.cs.roots.data.es.ResponseTimeHistoryQuery;
import edu.ucsb.cs.roots.data.es.ResponseTimeSummaryQuery;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static com.google.common.base.Preconditions.checkArgument;

public class ElasticSearchDataStore implements DataStore {

    private static final ImmutableList<String> METHODS = ImmutableList.of(
            "GET", "POST", "PUT", "DELETE");
    private static final Gson GSON = new Gson();

    private final String elasticSearchHost;
    private final int elasticSearchPort;

    private final String accessLogIndex;
    private final String accessLogTimestampField;
    private final String accessLogMethodField;
    private final String accessLogPathField;
    private final String accessLogResponseTimeField;
    private final String benchmarkIndex;
    private final String benchmarkTimestampField;

    private final CloseableHttpClient httpClient;

    private ElasticSearchDataStore(Builder builder) {
        checkArgument(!Strings.isNullOrEmpty(builder.elasticSearchHost),
                "ElasticSearch host is required");
        checkArgument(builder.elasticSearchPort > 0 && builder.elasticSearchPort < 65535,
                "ElasticSearch port number is invalid");
        this.httpClient = HttpClients.createDefault();
        this.elasticSearchHost = builder.elasticSearchHost;
        this.elasticSearchPort = builder.elasticSearchPort;
        this.accessLogIndex = builder.accessLogIndex;
        this.accessLogTimestampField = builder.accessLogTimestampField;
        this.accessLogMethodField = builder.accessLogMethodField;
        this.accessLogPathField = builder.accessLogPathField;
        this.accessLogResponseTimeField = builder.accessLogResponseTimeField;
        this.benchmarkIndex = builder.benchmarkIndex;
        this.benchmarkTimestampField = builder.benchmarkTimestampField;
    }

    @Override
    public void destroy() {
        IOUtils.closeQuietly(httpClient);
    }

    @Override
    public ImmutableMap<String,ResponseTimeSummary> getResponseTimeSummary(
            String application, long start, long end) throws DataStoreException {
        checkArgument(!Strings.isNullOrEmpty(accessLogIndex), "Access log index is required");
        String query = ResponseTimeSummaryQuery.newBuilder()
                .setAccessLogTimestampField(accessLogTimestampField)
                .setAccessLogMethodField(accessLogMethodField)
                .setAccessLogPathField(accessLogPathField)
                .setAccessLogResponseTimeField(accessLogResponseTimeField)
                .setStart(start)
                .setEnd(end)
                .buildJsonString();
        String path = String.format("/%s/%s/_search", accessLogIndex, application);
        ImmutableMap.Builder<String,ResponseTimeSummary> builder = ImmutableMap.builder();
        try {
            JsonElement results = makeHttpCall(path, query);
            parseResponseTimeSummary(results, start, builder);
        } catch (IOException | URISyntaxException e) {
            throw new DataStoreException("Error while querying ElasticSearch", e);
        }
        return builder.build();
    }

    @Override
    public ImmutableListMultimap<String,ResponseTimeSummary> getResponseTimeHistory(
            String application, long start, long end, long period) throws DataStoreException {
        checkArgument(!Strings.isNullOrEmpty(accessLogIndex), "Access log index is required");
        String query = ResponseTimeHistoryQuery.newBuilder()
                .setAccessLogTimestampField(accessLogTimestampField)
                .setAccessLogMethodField(accessLogMethodField)
                .setAccessLogPathField(accessLogPathField)
                .setAccessLogResponseTimeField(accessLogResponseTimeField)
                .setStart(start)
                .setEnd(end)
                .setPeriod(period)
                .buildJsonString();
        String path = String.format("/%s/%s/_search", accessLogIndex, application);
        ImmutableListMultimap.Builder<String,ResponseTimeSummary> builder = ImmutableListMultimap.builder();
        try {
            JsonElement results = makeHttpCall(path, query);
            parseResponseTimeHistory(results, builder);
        } catch (IOException | URISyntaxException e) {
            throw new DataStoreException("Error while querying ElasticSearch", e);
        }
        return builder.build();
    }

    @Override
    public ImmutableListMultimap<String, AccessLogEntry> getBenchmarkResults(
            String application, long start, long end) throws DataStoreException {
        checkArgument(!Strings.isNullOrEmpty(benchmarkIndex), "Benchmark index is required");
        String query = BenchmarkResultsQuery.newBuilder()
                .setBenchmarkTimestampField(benchmarkTimestampField)
                .setStart(start)
                .setEnd(end)
                .buildJsonString();
        String path = String.format("/%s/%s/_search", benchmarkIndex, application);
        ImmutableListMultimap.Builder<String,AccessLogEntry> builder = ImmutableListMultimap.builder();
        try {
            JsonElement results = makeHttpCall(path, query);
            parseBenchmarkResults(results, builder);
        } catch (IOException | URISyntaxException e) {
            throw new DataStoreException("Error while querying ElasticSearch", e);
        }
        return builder.build();
    }

    @Override
    public void recordBenchmarkResult(AccessLogEntry entry) throws DataStoreException {
        checkArgument(!Strings.isNullOrEmpty(benchmarkIndex), "Benchmark index is required");
        String path = String.format("/%s/%s", benchmarkIndex, entry.getApplication());
        try {
            makeHttpCall(path, GSON.toJson(entry));
        } catch (IOException | URISyntaxException e) {
            throw new DataStoreException("Error while querying ElasticSearch", e);
        }
    }

    private void parseResponseTimeSummary(JsonElement element, long timestamp,
                                          ImmutableMap.Builder<String,ResponseTimeSummary> builder) {
        JsonArray methods = element.getAsJsonObject().getAsJsonObject("aggregations")
                .getAsJsonObject("methods").getAsJsonArray("buckets");
        for (int i = 0; i < methods.size(); i++) {
            JsonObject method = methods.get(i).getAsJsonObject();
            String methodName = method.get("key").getAsString().toUpperCase();
            if (!METHODS.contains(methodName)) {
                continue;
            }
            JsonArray paths = method.getAsJsonObject("paths").getAsJsonArray("buckets");
            for (int j = 0; j < paths.size(); j++) {
                JsonObject path = paths.get(j).getAsJsonObject();
                String key = methodName + " " + path.get("key").getAsString();
                builder.put(key, newResponseTimeSummary(timestamp, path));
            }
        }
    }

    private void parseResponseTimeHistory(JsonElement element,
                                          ImmutableListMultimap.Builder<String,ResponseTimeSummary> builder) {
        JsonArray methods = element.getAsJsonObject().getAsJsonObject("aggregations")
                .getAsJsonObject("methods").getAsJsonArray("buckets");
        for (int i = 0; i < methods.size(); i++) {
            JsonObject method = methods.get(i).getAsJsonObject();
            String methodName = method.get("key").getAsString().toUpperCase();
            if (!METHODS.contains(methodName)) {
                continue;
            }
            JsonArray paths = method.getAsJsonObject("paths").getAsJsonArray("buckets");
            for (int j = 0; j < paths.size(); j++) {
                JsonObject path = paths.get(j).getAsJsonObject();
                String key = methodName + " " + path.get("key").getAsString();
                JsonArray periods = path.getAsJsonObject("periods").getAsJsonArray("buckets");
                for (int k = 0; k < periods.size(); k++) {
                    JsonObject period = periods.get(k).getAsJsonObject();
                    double count = period.get("doc_count").getAsDouble();
                    if (count > 0) {
                        long timestamp = period.get("key").getAsLong();
                        builder.put(key, newResponseTimeSummary(timestamp, period));
                    }
                }
            }
        }
    }

    private void parseBenchmarkResults(JsonElement element,
                                          ImmutableListMultimap.Builder<String,AccessLogEntry> builder) {
        JsonArray hits = element.getAsJsonObject().getAsJsonObject("hits")
                .getAsJsonArray("hits");
        for (int i = 0; i < hits.size(); i++) {
            JsonObject hit = hits.get(i).getAsJsonObject().getAsJsonObject("_source");
            AccessLogEntry entry = new AccessLogEntry(hit.get(benchmarkTimestampField).getAsLong(),
                    hit.get("application").getAsString(),
                    hit.get("method").getAsString(),
                    hit.get("path").getAsString(),
                    hit.get("responseTime").getAsInt());
            builder.put(entry.getRequestType(), entry);
        }
    }

    private ResponseTimeSummary newResponseTimeSummary(long timestamp, JsonObject bucket) {
        double responseTime = bucket.getAsJsonObject("avg_time").get("value")
                .getAsDouble() * 1000.0;
        double requestCount = bucket.get("doc_count").getAsDouble();
        return new ResponseTimeSummary(timestamp, responseTime, requestCount);
    }

    private JsonElement makeHttpCall(String path, String json) throws IOException, URISyntaxException {
        URI uri = new URI("http", null, elasticSearchHost, elasticSearchPort, path, null, null);
        HttpPost post = new HttpPost(uri);
        post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        return httpClient.execute(post, new ElasticSearchResponseHandler());
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    private static class ElasticSearchResponseHandler implements ResponseHandler<JsonElement> {
        @Override
        public JsonElement handleResponse(HttpResponse response) throws IOException {
            int status = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            if (status != 200 && status != 201) {
                String error = entity != null ? EntityUtils.toString(entity) : null;
                throw new ClientProtocolException("Unexpected status code: " + status
                        + "; response: " + error);
            }

            if (entity == null) {
                return null;
            }
            JsonParser parser = new JsonParser();
            ContentType contentType = ContentType.getOrDefault(entity);
            Charset charset = contentType.getCharset() != null ?
                    contentType.getCharset() : Charset.defaultCharset();
            return parser.parse(new InputStreamReader(entity.getContent(), charset));
        }
    }

    public static class Builder {

        private String elasticSearchHost;
        private int elasticSearchPort;
        private String accessLogIndex;
        private String accessLogTimestampField = "@timestamp";
        private String accessLogMethodField = "http_verb";
        private String accessLogPathField = "http_request.raw";
        private String accessLogResponseTimeField = "time_duration";
        private String benchmarkIndex;
        private String benchmarkTimestampField = "timestamp";

        private Builder() {
        }

        public Builder setElasticSearchHost(String elasticSearchHost) {
            this.elasticSearchHost = elasticSearchHost;
            return this;
        }

        public Builder setElasticSearchPort(int elasticSearchPort) {
            this.elasticSearchPort = elasticSearchPort;
            return this;
        }

        public Builder setAccessLogIndex(String accessLogIndex) {
            this.accessLogIndex = accessLogIndex;
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

        public Builder setBenchmarkIndex(String benchmarkIndex) {
            this.benchmarkIndex = benchmarkIndex;
            return this;
        }

        public Builder setBenchmarkTimestampField(String benchmarkTimestampField) {
            this.benchmarkTimestampField = benchmarkTimestampField;
            return this;
        }

        public ElasticSearchDataStore build() {
            return new ElasticSearchDataStore(this);
        }
    }

    public static void main(String[] args) throws DataStoreException {
        ElasticSearchDataStore dataStore = ElasticSearchDataStore.newBuilder()
                .setElasticSearchHost("128.111.179.226")
                .setElasticSearchPort(9200)
                .setAccessLogIndex("nginx")
                .setBenchmarkIndex("app-benchmarking")
                .build();

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2015, Calendar.NOVEMBER, 16, 0, 0, 0);
        Date start = cal.getTime();
        cal.set(2015, Calendar.NOVEMBER, 17, 0, 0, 0);
        Date end = cal.getTime();
        ImmutableListMultimap<String,AccessLogEntry> history =
                dataStore.getBenchmarkResults("foo", 0, System.currentTimeMillis());
        history.keySet().stream().forEach(k -> history.get(k)
                .forEach(v -> System.out.println(k + " " + v.getResponseTime())));

        dataStore.recordBenchmarkResult(new AccessLogEntry(System.currentTimeMillis(),
                "foo", "GET", "/", 10));
        dataStore.destroy();
    }
}
