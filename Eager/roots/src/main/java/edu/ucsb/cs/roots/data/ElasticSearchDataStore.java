package edu.ucsb.cs.roots.data;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import static com.google.common.base.Preconditions.checkArgument;

public class ElasticSearchDataStore implements DataStore {

    private static final ImmutableList<String> METHODS = ImmutableList.of(
            "GET", "POST", "PUT", "DELETE");

    private final String elasticSearchHost;
    private final int elasticSearchPort;
    private final String accessLogIndex;
    private final String accessLogTimestampField;
    private final String accessLogMethodField;
    private final String accessLogPathField;
    private final String accessLogResponseTimeField;
    private final CloseableHttpClient httpClient;

    private ElasticSearchDataStore(Builder builder) {
        checkArgument(!Strings.isNullOrEmpty(builder.accessLogIndex),
                "Access log index is required");
        checkArgument(!Strings.isNullOrEmpty(builder.elasticSearchHost),
                "ElasticSearch host is required");
        checkArgument(builder.elasticSearchPort > 0 && builder.elasticSearchPort < 65535,
                "ElasticSearch port number is invalid");
        checkArgument(!Strings.isNullOrEmpty(builder.accessLogTimestampField),
                "Timestamp field name for access log index is required");
        checkArgument(!Strings.isNullOrEmpty(builder.accessLogMethodField),
                "Method field name for access log index is required");
        checkArgument(!Strings.isNullOrEmpty(builder.accessLogPathField),
                "Path field name for access log index is required");
        checkArgument(!Strings.isNullOrEmpty(builder.accessLogResponseTimeField),
                "Response time field name for access log index is required");
        this.httpClient = HttpClients.createDefault();
        this.elasticSearchHost = builder.elasticSearchHost;
        this.elasticSearchPort = builder.elasticSearchPort;
        this.accessLogIndex = builder.accessLogIndex;
        this.accessLogTimestampField = builder.accessLogTimestampField;
        this.accessLogMethodField = builder.accessLogMethodField;
        this.accessLogPathField = builder.accessLogPathField;
        this.accessLogResponseTimeField = builder.accessLogResponseTimeField;
    }

    @Override
    public void destroy() {
        IOUtils.closeQuietly(httpClient);
    }

    @Override
    public ImmutableMap<String, ResponseTimeSummary> getResponseTimeSummary(
            String application, long start, long end) throws DataStoreException {
        String query = String.format(ElasticSearchTemplates.RESPONSE_TIME_SUMMARY_QUERY,
                accessLogTimestampField, start, end, accessLogMethodField, accessLogPathField,
                accessLogResponseTimeField);
        String path = String.format("/%s/%s/_search", accessLogIndex, application);
        ImmutableMap.Builder<String,ResponseTimeSummary> builder = ImmutableMap.builder();
        try {
            JsonElement results = makeHttpCall(elasticSearchHost, elasticSearchPort, path, query);
            parseResponseTimeSummary(results, start, builder);
        } catch (IOException | URISyntaxException e) {
            throw new DataStoreException("Error while querying ElasticSearch", e);
        }
        return builder.build();
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

    private ResponseTimeSummary newResponseTimeSummary(long timestamp, JsonObject bucket) {
        double responseTime = bucket.getAsJsonObject("avg_time").get("value")
                .getAsDouble() * 1000.0;
        double requestCount = bucket.get("doc_count").getAsDouble();
        return new ResponseTimeSummary(timestamp, responseTime, requestCount);
    }

    @Override
    public ImmutableMap<String, ImmutableList<ResponseTimeSummary>> getResponseTimeHistory(
            String application, long start, long end, long period) {
        return null;
    }

    @Override
    public ImmutableMap<String, ImmutableList<AccessLogEntry>> getBenchmarkResults(
            String application, long start, long end) {
        return null;
    }

    private JsonElement makeHttpCall(String host, int port, String path,
                              String json) throws IOException, URISyntaxException {
        URI uri = new URI("http", null, host, port, path, null, null);
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
            if (status != 200) {
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

        public ElasticSearchDataStore build() {
            return new ElasticSearchDataStore(this);
        }
    }

    public static void main(String[] args) throws DataStoreException {
        ElasticSearchDataStore dataStore = ElasticSearchDataStore.newBuilder()
                .setElasticSearchHost("128.111.179.159")
                .setElasticSearchPort(9200)
                .setAccessLogIndex("nginx")
                .build();

        Calendar cal = Calendar.getInstance();
        cal.set(2015, Calendar.NOVEMBER, 01);
        Date start = cal.getTime();
        cal.set(2015, Calendar.NOVEMBER, 30);
        Date end = cal.getTime();
        ImmutableMap<String,ResponseTimeSummary> result = dataStore.getResponseTimeSummary(
                "watchtower", start.getTime(), end.getTime());
        result.forEach((k,v) -> System.out.println(k + " " + v.getMeanResponseTime()
                + " " + v.getRequestCount()));
        dataStore.destroy();
    }
}
