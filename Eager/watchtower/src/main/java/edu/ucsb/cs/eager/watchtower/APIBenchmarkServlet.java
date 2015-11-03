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

package edu.ucsb.cs.eager.watchtower;

import edu.ucsb.cs.eager.watchtower.benchmark.APIBenchmark;
import edu.ucsb.cs.eager.watchtower.benchmark.DatastoreBenchmark;
import edu.ucsb.cs.eager.watchtower.benchmark.MemcacheBenchmark;
import edu.ucsb.cs.eager.watchtower.persistence.CloudDataPointStore;
import edu.ucsb.cs.eager.watchtower.persistence.DataPointStore;
import edu.ucsb.cs.eager.watchtower.persistence.ElasticSearchDataPointStore;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class APIBenchmarkServlet extends HttpServlet {

    private static final APIBenchmark[] benchmarks = new APIBenchmark[]{
        new DatastoreBenchmark(),
        new MemcacheBenchmark(),
    };

    private DataPointStore dataPointStore;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init();
        ServletContext context = config.getServletContext();
        String storeImpl = context.getInitParameter("dataPointStore");
        if ("elk".equals(storeImpl)) {
            storeImpl = ElasticSearchDataPointStore.class.getName();
        } else {
            storeImpl = CloudDataPointStore.class.getName();
        }
        try {
            Class<? extends DataPointStore> clazz = Class.forName(storeImpl)
                    .asSubclass(DataPointStore.class);
            dataPointStore = clazz.getConstructor(ServletContext.class).newInstance(context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {

        long timestamp = System.currentTimeMillis();
        BenchmarkContext context = new BenchmarkContext();
        if (!context.isInitialized()) {
            for (APIBenchmark b : benchmarks) {
                b.init();
            }
            context.setInitialized(true);
            if (context.save()) {
                JSONUtils.sendStatusMessage("Benchmarks initialized", resp);
            } else {
                resp.sendError(500, "Failed to save benchmark context");
            }
            return;
        }

        Map<String,Map<String,Integer>> results = new HashMap<>();
        DataPoint p = new DataPoint(timestamp);
        for (APIBenchmark b : benchmarks) {
            Map<String,Integer> data = b.benchmark();
            p.putAll(data);
            results.put(b.getName(), data);
        }

        if (context.isFirstRecord() || !context.isCollectionEnabled()) {
            // Always drop the very first data point collected.
            // This is almost always an outlier.
            context.setFirstRecord(false);
            if (context.save()) {
                JSONUtils.serializeMap(results, resp);
            } else {
                resp.sendError(500, "Failed to save benchmark context");
            }
        } else if (dataPointStore.save(p)) {
            JSONUtils.serializeMap(results, resp);
        } else {
            resp.sendError(500, "Failed to save benchmark data point");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req,
                          HttpServletResponse resp) throws ServletException, IOException {
        String enabled = req.getParameter("collectionEnabled");
        BenchmarkContext context = new BenchmarkContext();
        context.setCollectionEnabled(Boolean.parseBoolean(enabled));
        if (context.save()) {
            JSONUtils.serializeCollectionStatus(context.isCollectionEnabled(), resp);
        } else {
            resp.sendError(500, "Failed to save benchmark context");
        }
    }
}
