package edu.ucsb.cs.roots.data;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class ElasticSearchDataStore extends DataStore {

    private final String accessLogQueryTemplate;

    private final String elasticSearchHost;
    private final int elasticSearchPort;
    private final String accessLogIndex;
    private final CloseableHttpClient httpClient;

    private ElasticSearchDataStore(Builder builder) {
        checkArgument(!Strings.isNullOrEmpty(builder.accessLogIndex),
                "Access log index is required");
        checkArgument(!Strings.isNullOrEmpty(builder.elasticSearchHost),
                "ElasticSearch host is required");
        checkArgument(builder.elasticSearchPort > 0 && builder.elasticSearchPort < 65535,
                "ElasticSearch port number is invalid");
        this.httpClient = HttpClients.createDefault();
        this.elasticSearchHost = builder.elasticSearchHost;
        this.elasticSearchPort = builder.elasticSearchPort;
        this.accessLogIndex = builder.accessLogIndex;

        try {
            this.accessLogQueryTemplate = loadTemplate("access_log_query.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String loadTemplate(String name) throws IOException {
        try (InputStream in = ElasticSearchDataStore.class.getResourceAsStream(name)) {
            checkNotNull(in, "Failed to load resource: %s", name);
            return IOUtils.toString(in);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        IOUtils.closeQuietly(httpClient);
    }

    @Override
    public ImmutableMap<String, ResponseTimeSummary> getResponseTimeSummary(
            String application, long start, long end) {

        String query = String.format(accessLogQueryTemplate, start, end);
        String path = String.format("/%s/%s/_search", accessLogIndex, application);
        try {
            JsonElement response = makeHttpCall(elasticSearchHost, elasticSearchPort, path, query);
            System.out.println(response.toString());
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
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
        System.out.println(uri.toString() + "    " + json);
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
            if (status != 200) {
                System.out.println(EntityUtils.toString(response.getEntity()));
                throw new ClientProtocolException("Unexpected status code: " + status);
            }
            HttpEntity entity = response.getEntity();
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

        public ElasticSearchDataStore build() {
            return new ElasticSearchDataStore(this);
        }
    }

    public static void main(String[] args) {
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
        dataStore.getResponseTimeSummary("watchtower", start.getTime(), end.getTime());
        dataStore.destroy();
    }
}
