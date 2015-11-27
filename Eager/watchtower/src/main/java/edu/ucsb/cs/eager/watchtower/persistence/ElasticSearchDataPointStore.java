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

import com.google.appengine.labs.repackaged.org.json.JSONObject;
import edu.ucsb.cs.eager.watchtower.DataPoint;

import javax.servlet.ServletContext;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ElasticSearchDataPointStore extends DataPointStore {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM");

    private final String endpoint;
    private final String index;
    private final String type;

    public ElasticSearchDataPointStore(ServletContext context) {
        super(context);
        endpoint = getParameter(context, "elkEndpoint", null);
        if (endpoint == null) {
            throw new IllegalArgumentException("ELK endpoint not configured");
        }
        index = getParameter(context, "elkIndex", "watchtower-prod");
        type = getParameter(context, "elkType", "appengine");
        if (index.contains("/") || type.contains("/")) {
            throw new IllegalArgumentException("Index and type must not contain '/'");
        }
    }

    private String getParameter(ServletContext context, String name, String def) {
        String value = context.getInitParameter(name);
        if (value == null || "".equals(value)) {
            value = def;
        }
        return value;
    }

    @Override
    public boolean save(DataPoint p) {
        Map<String,Object> json = new HashMap<>();
        json.put("timestamp", p.getTimestamp());
        json.put("values", p.getData());
        String indexName = index + "_" + DATE_FORMAT.format(new Date());
        try {
            return sendToElasticSearch(indexName, json);
        } catch (IOException e) {
            return false;
        }
    }

    private boolean sendToElasticSearch(String indexName, Map<String,Object> json) throws IOException {
        URL url = new URL(endpoint + "/" + indexName + "/" + type);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())) {
            writer.write(new JSONObject(json).toString());
        }

        int httpStatus = connection.getResponseCode();
        if (httpStatus >= 200 && httpStatus < 400) {
            try (InputStream in = connection.getInputStream()) {
                byte[] data = new byte[1024];
                while (in.read(data) != -1) {
                }
            }
        }
        return httpStatus == HttpURLConnection.HTTP_CREATED;
    }
}
