package edu.ucsb.cs.roots.data;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import edu.ucsb.cs.roots.data.es.BenchmarkResultsQuery;
import edu.ucsb.cs.roots.data.es.ResponseTimeHistoryQuery;
import edu.ucsb.cs.roots.data.es.ResponseTimeSummaryQuery;
import edu.ucsb.cs.roots.data.es.ScrollQuery;
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
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;

public class ElasticSearchDataStore implements DataStore {

    private static final ImmutableList<String> METHODS = ImmutableList.of(
            "GET", "POST", "PUT", "DELETE");
    private static final Gson GSON = new Gson();

    private static final String ACCESS_LOG_TIMESTAMP = "field.accessLog.timestamp";
    private static final String ACCESS_LOG_METHOD = "field.accessLog.method";
    private static final String ACCESS_LOG_PATH = "field.accessLog.path";
    private static final String ACCESS_LOG_RESPONSE_TIME = "field.accessLog.responseTime";
    private static final String BENCHMARK_TIMESTAMP = "field.benchmark.timestamp";
    private static final String BENCHMARK_METHOD = "field.benchmark.method";
    private static final String BENCHMARK_PATH = "field.benchmark.path";
    private static final String BENCHMARK_RESPONSE_TIME = "field.benchmark.responseTime";

    private static final ImmutableMap<String, String> DEFAULT_FIELD_MAPPINGS =
            ImmutableMap.<String, String>builder()
                    .put(ACCESS_LOG_TIMESTAMP, "@timestamp")
                    .put(ACCESS_LOG_METHOD, "http_verb")
                    .put(ACCESS_LOG_PATH, "http_request.raw")
                    .put(ACCESS_LOG_RESPONSE_TIME, "time_duration")
                    .put(BENCHMARK_TIMESTAMP, "timestamp")
                    .put(BENCHMARK_METHOD, "method")
                    .put(BENCHMARK_PATH, "path")
                    .put(BENCHMARK_RESPONSE_TIME, "responseTime")
                    .build();

    private final String elasticSearchHost;
    private final int elasticSearchPort;

    private final String accessLogIndex;
    private final String benchmarkIndex;

    private final ImmutableMap<String,String> fieldMappings;

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
        this.benchmarkIndex = builder.benchmarkIndex;
        this.fieldMappings = ImmutableMap.copyOf(builder.fieldMappings);
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
                .setAccessLogTimestampField(fieldMappings.get(ACCESS_LOG_TIMESTAMP))
                .setAccessLogMethodField(fieldMappings.get(ACCESS_LOG_METHOD))
                .setAccessLogPathField(fieldMappings.get(ACCESS_LOG_PATH))
                .setAccessLogResponseTimeField(fieldMappings.get(ACCESS_LOG_RESPONSE_TIME))
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
                .setAccessLogTimestampField(fieldMappings.get(ACCESS_LOG_TIMESTAMP))
                .setAccessLogMethodField(fieldMappings.get(ACCESS_LOG_METHOD))
                .setAccessLogPathField(fieldMappings.get(ACCESS_LOG_PATH))
                .setAccessLogResponseTimeField(fieldMappings.get(ACCESS_LOG_RESPONSE_TIME))
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
    public ImmutableListMultimap<String, BenchmarkResult> getBenchmarkResults(
            String application, long start, long end) throws DataStoreException {
        checkArgument(!Strings.isNullOrEmpty(benchmarkIndex), "Benchmark index is required");
        String query = BenchmarkResultsQuery.newBuilder()
                .setBenchmarkTimestampField(fieldMappings.get(BENCHMARK_TIMESTAMP))
                .setStart(start)
                .setEnd(end)
                .buildJsonString();
        String path = String.format("/%s/%s/_search", benchmarkIndex, application);
        ImmutableListMultimap.Builder<String,BenchmarkResult> builder = ImmutableListMultimap.builder();
        try {
            JsonElement results = makeHttpCall(path, "scroll=1m", query);
            String scrollId = results.getAsJsonObject().get("_scroll_id").getAsString();
            long total = results.getAsJsonObject().getAsJsonObject("hits").get("total").getAsLong();
            long received = 0L;
            while (true) {
                received += parseBenchmarkResults(application, results, builder);
                if (received >= total) {
                    break;
                }
                results = makeHttpCall("/_search/scroll", ScrollQuery.build(scrollId));
            }
        } catch (IOException | URISyntaxException e) {
            throw new DataStoreException("Error while querying ElasticSearch", e);
        }
        return builder.build();
    }

    @Override
    public void recordBenchmarkResult(BenchmarkResult result) throws DataStoreException {
        checkArgument(!Strings.isNullOrEmpty(benchmarkIndex), "Benchmark index is required");
        String path = String.format("/%s/%s", benchmarkIndex, result.getApplication());
        try {
            makeHttpCall(path, GSON.toJson(result));
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

    private int parseBenchmarkResults(String application, JsonElement element,
                                       ImmutableListMultimap.Builder<String,BenchmarkResult> builder) {
        JsonArray hits = element.getAsJsonObject().getAsJsonObject("hits")
                .getAsJsonArray("hits");
        for (int i = 0; i < hits.size(); i++) {
            JsonObject hit = hits.get(i).getAsJsonObject().getAsJsonObject("_source");
            BenchmarkResult entry = new BenchmarkResult(
                    hit.get(fieldMappings.get(BENCHMARK_TIMESTAMP)).getAsLong(),
                    application,
                    hit.get(fieldMappings.get(BENCHMARK_METHOD)).getAsString(),
                    hit.get(fieldMappings.get(BENCHMARK_PATH)).getAsString(),
                    hit.get(fieldMappings.get(BENCHMARK_RESPONSE_TIME)).getAsInt());
            builder.put(entry.getRequestType(), entry);
        }
        return hits.size();
    }

    private ResponseTimeSummary newResponseTimeSummary(long timestamp, JsonObject bucket) {
        double responseTime = bucket.getAsJsonObject("avg_time").get("value")
                .getAsDouble() * 1000.0;
        double requestCount = bucket.get("doc_count").getAsDouble();
        return new ResponseTimeSummary(timestamp, responseTime, requestCount);
    }

    private JsonElement makeHttpCall(String path,
                                     String json) throws IOException, URISyntaxException {
        return makeHttpCall(path, null, json);
    }

    private JsonElement makeHttpCall(String path, String query,
                                     String json) throws IOException, URISyntaxException {
        URI uri = new URI("http", null, elasticSearchHost, elasticSearchPort, path, query, null);
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
        private String benchmarkIndex;
        private final Map<String,String> fieldMappings = new HashMap<>(DEFAULT_FIELD_MAPPINGS);

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

        public Builder setBenchmarkIndex(String benchmarkIndex) {
            this.benchmarkIndex = benchmarkIndex;
            return this;
        }

        public Builder setFieldMapping(String field, String mapping) {
            fieldMappings.put(field, mapping);
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
        ImmutableMap<String,ResponseTimeSummary> history =
                dataStore.getResponseTimeSummary("watchtower", start.getTime(), end.getTime());
        history.forEach((k,v) -> System.out.println(k + " " + v.getMeanResponseTime() +
                " " + v.getRequestCount()));
        dataStore.recordBenchmarkResult(new BenchmarkResult(System.currentTimeMillis(),
                "foo", "GET", "/", 10));
        dataStore.destroy();
    }
}
