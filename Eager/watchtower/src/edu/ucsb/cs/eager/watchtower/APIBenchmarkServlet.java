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
        DataPoint p = new DataPoint(timestamp);
        for (APIBenchmark b : benchmarks) {
            Map<String,Integer> data = b.benchmark();
            p.putAll(data);
            results.put(b.getName(), data);
        }

        p.save();
        JSONUtils.serialize(results, resp);
    }

}
