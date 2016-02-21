/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package edu.ucsb.cs.elkagent;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ElkAgent {

    private final URI uri;
    private final CloseableHttpClient client;
    private final ExecutorService exec;

    public ElkAgent(String url, String index, String type) {
        this.uri = URI.create(url + "/" + index + "/" + type);
        this.client = HttpClients.createDefault();
        this.exec = Executors.newCachedThreadPool();
    }

    public void report() {
        exec.submit(new Reporter());
    }

    public void report(Map<String,Object> values) {
        exec.submit(new Reporter(values));
    }

    public void close() {
        exec.shutdown();
        try {
            client.close();
        } catch (IOException ignored) {
        }
    }

    private class Reporter implements Runnable {

        private final Map<String,Object> values;

        public Reporter(Map<String, Object> values) {
            this.values = values;
        }

        public Reporter() {
            this(null);
        }

        public void run() {
            HttpPost post = new HttpPost(uri);
            String json = MonitoredValues.newInstance().withMemoryUsageData().with(values)
                    .withTimestamp("timestamp").toJson();
            post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            try (CloseableHttpResponse response = client.execute(post)) {
                System.out.println("Received response:" + response.getStatusLine().getStatusCode());
                EntityUtils.consumeQuietly(response.getEntity());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
