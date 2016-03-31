package edu.ucsb.cs.roots.bm;

import com.google.common.base.Strings;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;

import static com.google.common.base.Preconditions.checkArgument;

public final class BenchmarkCall {

    private final String method;
    private final URI url;

    public BenchmarkCall(String method, String url) {
        checkArgument(!Strings.isNullOrEmpty(method), "HTTP method is required");
        checkArgument(!Strings.isNullOrEmpty(url), "HTTP URL is required");
        this.method = method;
        this.url = URI.create(url);
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        String path = url.getPath();
        if (Strings.isNullOrEmpty(path)) {
            return "/";
        }
        return path;
    }

    public long execute(CloseableHttpClient client) throws IOException {
        HttpUriRequest request;
        if (method.equals("GET")) {
            request = new HttpGet(url);
        } else {
            throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        HttpHost host = URIUtils.extractHost(url);
        long start = System.nanoTime();
        try (CloseableHttpResponse response = client.execute(host, request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            long end = System.nanoTime();
            return Math.floorDiv(end - start, 1000000L);
        }
    }

}
