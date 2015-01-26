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

import org.json.JSONArray;
import org.json.JSONObject;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.util.*;

public class SimpleQBETSPredictor {

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
            if (config.isEnabledMethod(m.getName())) {
                Prediction prediction = predictExecTime(m, config, cache);
                System.out.format("%-" + maxLength + "s%5d%15s\n", m.getName(), m.getPaths().size(),
                        prediction.toString());
            }
        }
    }

    private static Prediction predictExecTime(MethodInfo method, PredictionConfig config,
                                              QuantileCache cache) throws IOException {

        List<Path> pathsOfInterest = PredictionUtils.getPathsOfInterest(method);
        if (pathsOfInterest.size() == 0) {
            return new Prediction("------");
        }

        Set<Integer> pathLengths = new HashSet<>();
        Set<String> uniqueOps = new HashSet<>();
        for (Path path : pathsOfInterest) {
            pathLengths.add(path.size());
            for (APICall call : path.calls()) {
                uniqueOps.add(call.getId());
            }
        }

        // Compute quantiles for each API call, under different API call counts
        for (Integer pathLength : pathLengths) {
            Set<String> reducedOps = new HashSet<>();
            for (String op : uniqueOps) {
                // Only request combinations, that are not in the cache already
                if (!cache.contains(op, pathLength)) {
                    reducedOps.add(op);
                }
            }

            if (reducedOps.isEmpty()) {
                continue;
            }

            // If there are n API calls in the path, we need to compute the n-th root
            // quantile for each API call
            double adjustedQuantile = Math.pow(config.getQuantile(), 1.0/pathLength);
            Map<String,Integer> map = getQuantiles(config, adjustedQuantile, reducedOps);
            for (Map.Entry<String,Integer> entry : map.entrySet()) {
                cache.put(entry.getKey(), pathLength, entry.getValue());
            }
        }

        Prediction[] predictions = new Prediction[pathsOfInterest.size()];
        for (int i = 0; i < pathsOfInterest.size(); i++) {
            try {
                predictions[i] = analyzePath(pathsOfInterest.get(i), cache);
            } catch (Exception e) {
                return new Prediction("error");
            }
        }

        // And return the most expensive one
        return PredictionUtils.max(predictions);
    }

    private static Prediction analyzePath(Path path, QuantileCache cache) {
        double total = 0.0;
        for (APICall call : path.calls()) {
            total += cache.get(call.getId(), path.size());
        }
        return new Prediction(total);
    }

    private static Map<String,Integer> getQuantiles(PredictionConfig config,
                                                    double quantile,
                                                    Collection<String> ops) throws IOException {
        JSONObject msg = new JSONObject();
        msg.put("quantile", quantile);
        msg.put("confidence", config.getConfidence());
        msg.put("operations", new JSONArray(ops));
        msg.put("start", config.getStart());
        msg.put("end", config.getEnd());

        JSONObject resp = HttpUtils.doPost(config.getBenchmarkDataSvc() + "/predict", msg);
        Map<String,Integer> quantiles = new HashMap<>();
        Iterator keys = resp.keys();
        while (keys.hasNext()) {
            String k = (String) keys.next();
            quantiles.put(k, resp.getInt(k));
        }
        return quantiles;
    }

}
