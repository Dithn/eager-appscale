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

import com.google.appengine.repackaged.com.google.gson.Gson;
import edu.ucsb.cs.eager.watchtower.DataPoint;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElasticSearchDataPointStore extends DataPointStore {

    private static final Gson gson = new Gson();

    private final URL url;

    public ElasticSearchDataPointStore(ServletContext context) {
        String endpoint = context.getInitParameter("elkEndpoint");
        if (endpoint == null || "".equals(endpoint)) {
            throw new RuntimeException("ELK endpoint not configured");
        }
        try {
            url = new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed URL", e);
        }
    }

    @Override
    public boolean save(DataPoint p) {
        Map<String,Object> json = new HashMap<>();
        json.put("timestamp", p.getTimestamp());
        json.putAll(p.getData());
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(gson.toJson(json));
            writer.close();

            boolean status = connection.getResponseCode() == HttpURLConnection.HTTP_CREATED;
            connection.disconnect();
            return status;
        } catch (IOException e) {
            return false;
        }
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
