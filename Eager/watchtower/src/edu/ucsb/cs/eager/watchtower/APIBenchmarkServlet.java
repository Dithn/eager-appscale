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

import com.google.appengine.api.datastore.*;
import edu.ucsb.cs.eager.watchtower.benchmark.APIBenchmark;
import edu.ucsb.cs.eager.watchtower.benchmark.DatastoreBenchmark;

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
    };

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {

        long timestamp = System.currentTimeMillis();
        Map<String,Map<String,Integer>> results = new HashMap<String, Map<String, Integer>>();
        for (APIBenchmark b : benchmarks) {
            results.put(b.getName(), b.benchmark());
        }

        saveDataPoint(results, timestamp);
        JSONUtils.serialize(results, resp);
    }

    private void saveDataPoint(Map<String,Map<String,Integer>> results, long timestamp) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Entity dataPoint = new Entity(Constants.DATA_POINT_KIND, Constants.DATA_POINT_PARENT);
        dataPoint.setProperty(Constants.DATA_POINT_TIMESTAMP, timestamp);
        for (Map<String,Integer> apiData : results.values()) {
            for (Map.Entry<String,Integer> entry : apiData.entrySet()) {
                dataPoint.setProperty(entry.getKey(), entry.getValue());
            }
        }
        datastore.put(dataPoint);
    }


}