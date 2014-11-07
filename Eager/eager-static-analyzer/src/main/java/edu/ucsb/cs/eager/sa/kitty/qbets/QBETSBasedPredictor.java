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

import edu.ucsb.cs.eager.sa.kitty.*;

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

    public static void predict(PredictionConfig config,
                               Collection<MethodInfo> methods) throws IOException {
        if (config.isAggregateTimeSeries()) {
            throw new NotImplementedException();
        }

        System.out.printf("\nQuantile: %.4f\nConfidence: %.4f\nClass: %s\n\n",
                config.getQuantile(), config.getConfidence(), config.getClazz());
        QuantileCache cache = new QuantileCache();
        int maxLength = 0;
        for (MethodInfo m : methods) {
            if (m.getName().length() > maxLength) {
                maxLength = m.getName().length();
            }
        }
        for (MethodInfo m : methods) {
            Prediction prediction = predictExecTime(m, config, cache);
            System.out.format("%-" + maxLength + "s%5d%15s\n", m.getName(), m.getPaths().size(),
                    prediction.toString());
        }
    }

    private static Prediction predictExecTime(MethodInfo method, PredictionConfig config,
                                              QuantileCache cache) throws IOException {

        List<List<APICall>> pathsOfInterest = PredictionUtils.getPathsOfInterest(method);
        if (pathsOfInterest.size() == 0) {
            return new Prediction("------");
        }

        Set<Integer> pathLengths = new HashSet<Integer>();
        Set<String> uniqueOps = new HashSet<String>();
        for (List<APICall> path : pathsOfInterest) {
            pathLengths.add(path.size());
            for (APICall call : path) {
                uniqueOps.add(call.getShortName());
            }
        }

        // Compute quantiles for each API call, under different API call counts
        for (Integer pathLength : pathLengths) {
            Set<String> reducedOps = new HashSet<String>();
            for (String op : uniqueOps) {
                // Only request combinations, that are not in the cache already
                if (!cache.contains(op, pathLength)) {
                    reducedOps.add(op);
                }
            }

            if (reducedOps.isEmpty()) {
                continue;
            }

            double adjustedQuantile = Math.pow(config.getQuantile(), 1.0/pathLength);
            Map<String,Integer> map = getQuantiles(config.getBenchmarkDataSvc(),
                    adjustedQuantile, config.getConfidence(), reducedOps);
            for (Map.Entry<String,Integer> entry : map.entrySet()) {
                cache.put(entry.getKey(), pathLength, entry.getValue());
            }
        }

        Prediction[] predictions = new Prediction[pathsOfInterest.size()];
        for (int i = 0; i < pathsOfInterest.size(); i++) {
            List<APICall> path = pathsOfInterest.get(i);
            predictions[i] = analyzePath(path, cache);
        }

        // And return the most expensive one
        return PredictionUtils.max(predictions);
    }

    private static Prediction analyzePath(List<APICall> path, QuantileCache cache) {
        double total = 0.0;
        for (APICall call : path) {
            total += cache.get(call.getShortName(), path.size());
        }
        return new Prediction(total);
    }

    private static Map<String,Integer> getQuantiles(String bmDataSvc,
                                                    double quantile,
                                                    double confidence,
                                                    Collection<String> ops) throws IOException {
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

            HttpResponse response = httpClient.execute(request);
            InputStream in = response.getEntity().getContent();
            StringBuilder sb = new StringBuilder();
            byte[] data = new byte[1024];
            int len;
            while ((len = in.read(data)) != -1) {
                sb.append(new String(data, 0, len));
            }
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException(sb.toString());
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
