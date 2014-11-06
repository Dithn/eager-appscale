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

package edu.ucsb.cs.eager.sa.kitty.qbets;

import edu.ucsb.cs.eager.sa.kitty.APICall;
import edu.ucsb.cs.eager.sa.kitty.MethodInfo;
import edu.ucsb.cs.eager.sa.kitty.Prediction;
import edu.ucsb.cs.eager.sa.kitty.PredictionConfig;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class QBETSBasedPredictor {

    public static void predict(PredictionConfig config, List<MethodInfo> methods) throws IOException {
        if (config.isAggregateTimeSeries()) {
            throw new NotImplementedException();
        }

        for (MethodInfo m : methods) {
            System.out.println(m.getName());
            for (int i = 0; i < m.getName().length(); i++) {
                System.out.print("=");
            }
            System.out.println();
            System.out.println("Total paths: " + m.getPaths().size());
            // TODO: read q,c from CLI
            System.out.println("Worst-case exec time: " + predictExecTime(m,
                    config.getBenchmarkDataSvc(), 0.95, 0.05));
            System.out.println();
        }
    }

    private static Prediction predictExecTime(MethodInfo method, String bmDataSvc,
                                        double quantile, double confidence) throws IOException {

        List<List<APICall>> pathsOfInterest = new ArrayList<List<APICall>>();
        for (List<APICall> p : method.getPaths()) {
            if (p.size() == 1 && p.get(0).getName().equals("-- No API Calls --")) {
                continue;
            }
            pathsOfInterest.add(p);
        }

        if (pathsOfInterest.size() == 0) {
            return new Prediction("No paths with API calls found");
        }

        Set<Integer> pathLengths = new HashSet<Integer>();
        Set<String> uniqueOps = new HashSet<String>();
        for (List<APICall> path : pathsOfInterest) {
            pathLengths.add(path.size());
            for (APICall call : path) {
                uniqueOps.add(call.getShortName());
            }
        }

        Map<Integer, Map<String,Integer>> quantiles = new HashMap<Integer, Map<String, Integer>>();
        for (Integer pathLength : pathLengths) {
            double adjustedQuantile = Math.pow(quantile, 1.0/pathLength);
            quantiles.put(pathLength, getQuantiles(bmDataSvc, adjustedQuantile, confidence, uniqueOps));
        }
        Prediction[] predictions = new Prediction[pathsOfInterest.size()];
        for (int i = 0; i < pathsOfInterest.size(); i++) {
            List<APICall> path = pathsOfInterest.get(i);
            predictions[i] = analyzePath(path, quantiles.get(path.size()));
        }

        // And return the most expensive one
        Prediction max = new Prediction(0.0);
        for (int i = 0; i < pathsOfInterest.size(); i++) {
            if (predictions[i].getValue() > max.getValue()) {
                max = predictions[i];
            }
        }
        return max;
    }

    private static Prediction analyzePath(List<APICall> path, Map<String,Integer> quantiles) {
        double total = 0.0;
        for (APICall call : path) {
            total += quantiles.get(call.getShortName());
        }
        return new Prediction(total);
    }

    private static Map<String,Integer> getQuantiles(String bmDataSvc, double quantile,
                                                    double confidence, Collection<String> ops) throws IOException {
        JSONObject msg = new JSONObject();
        msg.put("quantile", quantile);
        msg.put("confidence", confidence);
        msg.put("operations", new JSONArray(ops));

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        JSONObject svcResponse;
        try {
            HttpPost request = new HttpPost(bmDataSvc + "/predict");
            StringEntity params = new StringEntity(msg.toString());
            request.addHeader("content-type", "application/json");
            request.setEntity(params);

            System.out.println("Contacting benchmark data service... " + msg.toString());
            HttpResponse response = httpClient.execute(request);
            InputStream in = response.getEntity().getContent();
            StringBuilder sb = new StringBuilder();
            byte[] data = new byte[1024];
            int len;
            while ((len = in.read(data)) != -1) {
                sb.append(new String(data, 0, len));
            }
            svcResponse = new JSONObject(sb.toString());
        } finally {
            httpClient.close();
        }

        Map<String,Integer> quantiles = new HashMap<String, Integer>();
        Iterator keys = svcResponse.keys();
        while (keys.hasNext()) {
            String k = (String) keys.next();
            quantiles.put(k, svcResponse.getInt(k));
        }
        return quantiles;
    }

}
