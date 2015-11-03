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

package edu.ucsb.cs.eager.watchtower.persistence;

import com.google.gson.Gson;
import edu.ucsb.cs.eager.watchtower.DataPoint;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElasticSearchDataPointStore extends DataPointStore {

    private static final String INDEX_NAME = "logstash-watchtower";
    private static final String ELK_ENDPOINT = "elkEndpoint";
    private static final Gson gson = new Gson();

    private final String endpoint;

    public ElasticSearchDataPointStore(ServletContext context) {
        endpoint = context.getInitParameter(ELK_ENDPOINT);
        if (endpoint == null || "".equals(endpoint)) {
            throw new RuntimeException("ELK endpoint not configured");
        }
    }

    @Override
    public boolean save(DataPoint p) {
        try {
            URL url = new URL(endpoint + "/_bulk");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(toBulkIndexRequest(p));
            writer.close();

            boolean status = connection.getResponseCode() == HttpURLConnection.HTTP_OK;
            connection.disconnect();
            return status;
        } catch (IOException e) {
            return false;
        }
    }

    private String toBulkIndexRequest(DataPoint p) {
        Map<String,Map<String,Object>> serviceResults = new HashMap<>();
        for (Map.Entry<String,Integer> entry : p.getData().entrySet()) {
            String key = entry.getKey();
            String serviceName = key.substring(0, key.indexOf('_', 3));
            Map<String,Object> sr = serviceResults.get(serviceName);
            if (sr == null) {
                sr = new HashMap<>();
                sr.put("timestamp", p.getTimestamp());
                serviceResults.put(serviceName, sr);
            }
            sr.put(key, entry.getValue());
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String,Map<String,Object>> entry : serviceResults.entrySet()) {
            String op = String.format("{ \"index\" : { \"_index\" : \"%s\", \"_type\" : \"%s\" } }\n",
                    INDEX_NAME, entry.getKey());
            sb.append(op);
            sb.append(gson.toJson(entry.getValue())).append("\n");
        }
        return sb.toString();
    }

    @Override
    public List<DataPoint> getAll() {
        return null;
    }

    @Override
    public List<DataPoint> getRange(long start, int limit) {
        return null;
    }

    @Override
    public boolean restore(List<DataPoint> dataPoints) {
        return false;
    }

    @Override
    public void close() {
    }
}
