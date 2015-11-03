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

import edu.ucsb.cs.eager.watchtower.DataPoint;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import javax.servlet.ServletContext;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElasticSearchDataPointStore extends DataPointStore {

    private final TransportClient client;

    public ElasticSearchDataPointStore(ServletContext context) {
        String endpoints = context.getInitParameter("elkEndpoints");
        if (endpoints == null || "".equals(endpoints)) {
            throw new RuntimeException("ELK endpoints not configured");
        }
        client = TransportClient.builder().build();
        for (String endpoint : endpoints.split(",")) {
            String[] segments = endpoint.split(":");
            try {
                client.addTransportAddress(new InetSocketTransportAddress(
                        InetAddress.getByName(segments[0]), Integer.parseInt(segments[1])));
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean save(DataPoint p) {
        Map<String,Object> json = new HashMap<>();
        json.put("timestamp", p.getTimestamp());
        json.putAll(p.getData());
        IndexResponse response = client.prepareIndex("logstash-watchtower", "appengine")
                .setSource(json).get();
        return response.isCreated();
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
        client.close();
    }
}
