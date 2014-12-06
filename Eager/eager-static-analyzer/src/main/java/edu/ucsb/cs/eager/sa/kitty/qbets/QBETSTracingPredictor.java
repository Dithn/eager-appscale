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

import java.io.IOException;
import java.util.*;

/**
 * QBETSTracingPredictor obtains time series data for different API calls,
 * and performs an offline trace on them. For each entry in a time series,
 * it calculates 2 quantile predictions, and computes how often the actual
 * values fall within the predicted bounds.
 */
public class QBETSTracingPredictor {

    public static void predict(PredictionConfig config,
                               Collection<MethodInfo> methods) throws IOException {

        Set<String> ops = new HashSet<String>();
        for (MethodInfo m : methods) {
            for (Path path : m.getPaths()) {
                for (APICall call : path.calls()) {
                    ops.add(call.getId());
                }
            }
        }

        if (ops.isEmpty()) {
            System.out.println("No methods with API calls found.");
            return;
        }

        System.out.println("\nRetrieving time series data for " + ops.size() + " API " +
                PredictionUtils.pluralize(ops.size(), "call") + "...");
        TimeSeriesDataCache cache = new TimeSeriesDataCache(
                getTimeSeriesData(config.getBenchmarkDataSvc(), ops));
        System.out.println("Retrieved time series length: " + cache.getTimeSeriesLength());

        for (MethodInfo m : methods) {
            if (config.isEnabledMethod(m.getName())) {
                analyzeMethod(m, cache, config);
            }
        }
    }

    private static void analyzeMethod(MethodInfo method, TimeSeriesDataCache cache,
                                      PredictionConfig config) throws IOException {
        printTitle(method.getName(), '=');
        List<Path> pathsOfInterest = PredictionUtils.getPathsOfInterest(method);
        if (pathsOfInterest.size() == 0) {
            System.out.println("No paths with API calls found.");
            return;
        }

        System.out.println(pathsOfInterest.size() + PredictionUtils.pluralize(
                pathsOfInterest.size(), " path") + " with API calls found.");

        List<Path> uniquePaths = new ArrayList<Path>();
        uniquePaths.add(pathsOfInterest.get(0));
        for (int i = 1; i < pathsOfInterest.size(); i++) {
            Path current = pathsOfInterest.get(i);
            boolean matchFound = false;
            for (Path uniquePath : uniquePaths) {
                if (uniquePath.equivalent(current)) {
                    matchFound = true;
                    break;
                }
            }
            if (!matchFound) {
                uniquePaths.add(current);
            }
        }

        System.out.println(uniquePaths.size() + " unique " + PredictionUtils.pluralize(
                uniquePaths.size(), "path") + " with API calls found.");
        for (int i = 0; i < uniquePaths.size(); i++) {
            analyzePath(method, uniquePaths.get(i), i, cache, config);
        }
    }

    private static void analyzePath(MethodInfo method, Path path, int pathIndex,
                                    TimeSeriesDataCache cache, PredictionConfig config) throws IOException {
        printTitle("Path: " + pathIndex, '-');
        System.out.println("API Calls: " + path);

        int tsLength = cache.getTimeSeriesLength();
        int pathLength = path.size();
        double adjustedQuantile = Math.pow(config.getQuantile(), 1.0/pathLength);

        int minIndex = (int) (Math.log(config.getConfidence()) / Math.log(adjustedQuantile)) + 10;
        System.out.println("Minimum acceptable time series length: " + (minIndex + 1));

        // create new aggregate time series
        int[] aggregate = new int[tsLength];
        for (int i = 0; i < tsLength; i++) {
            for (APICall call : path.calls()) {
                aggregate[i] += cache.getTimeSeries(call)[i];
            }
        }

        int dataPoints = tsLength - minIndex;
        if (dataPoints <= 0) {
            System.out.println("Insufficient data in time series...");
            return;
        }
        System.out.println("Number of data points analyzed: " + dataPoints);

        // Approach 1
        for (APICall call : path.calls()) {
            if (!cache.containsQuantiles(call, path.size())) {
                System.out.println("Calculating quantiles for: " + call.getId() +
                        " (q = " + adjustedQuantile + "; c = " + config.getConfidence() + ")");
                int[] quantilePredictions = getQuantilePredictions(config.getBenchmarkDataSvc(),
                        cache.getTimeSeries(call), call.getId(), dataPoints,
                        adjustedQuantile, config.getConfidence());
                cache.putQuantiles(call, path.size(), quantilePredictions);
            }
        }

        int[] quantileSums = new int[dataPoints];
        int[] actualSums = new int[dataPoints];
        for (int i = 0; i < dataPoints; i++) {
            for (APICall call : path.calls()) {
                // Sum up the adjusted quantiles
                quantileSums[i] += cache.getQuantiles(call, path.size())[i];
                // Sum up the actual values from the original time series
                actualSums[i] += cache.getTimeSeries(call)[i + minIndex];
            }
        }

        // Approach 2
        if (!cache.containsQuantiles(path, path.size())) {
            int[] quantilePredictions = getQuantilePredictions(config.getBenchmarkDataSvc(),
                    aggregate, path.getId(), dataPoints, config.getQuantile(),
                    config.getConfidence());
            cache.putQuantiles(path, path.size(), quantilePredictions);
        }
        int[] aggregateQuantiles = cache.getQuantiles(path, path.size());

        TraceAnalysisResult[] results = new TraceAnalysisResult[dataPoints];
        for (int i = 0; i < dataPoints; i++) {
            results[i] = new TraceAnalysisResult();
            results[i].approach1 = quantileSums[i];
            results[i].approach2 = aggregateQuantiles[i];
            results[i].sum = actualSums[i];
        }

        System.out.println();
        int failures = 0;
        System.out.printf("[trace][method][path] index p1 p2 current  success success_rate\n");
        for (int i = 0; i < results.length; i++) {
            TraceAnalysisResult r = results[i];
            if (i > 0) {
                boolean success = r.sum < results[i - 1].approach2;
                if (!success) {
                    failures++;
                }
                double successRate = ((double)(i - failures) / i) * 100.0;
                System.out.printf("[trace][%s][%d] %4d %4d %4d %4d  %-5s %4.4f\n",
                        method.getName(), pathIndex, i + minIndex, r.approach1, r.approach2,
                        r.sum, success, successRate);
            } else {
                System.out.printf("[trace][%s][%d] %4d %4d %4d %4d  %-5s %-7s\n",
                        method.getName(), pathIndex, i + minIndex, r.approach1, r.approach2,
                        r.sum, "N/A", "N/A");
            }
        }
    }

    private static int[] getQuantilePredictions(String bmDataSvc, int[] ts, String name, int len,
                                      double quantile, double confidence) throws IOException {
        JSONObject msg = new JSONObject();
        msg.put("quantile", quantile);
        msg.put("confidence", confidence);
        msg.put("data", new JSONArray(ts));
        msg.put("name", name);

        JSONObject resp = HttpUtils.doPost(bmDataSvc + "/cpredict", msg);
        JSONArray array = resp.getJSONArray("Predictions");
        int[] result = new int[len];
        for (int i = 0; i < len; i++) {
            result[i] = array.getInt(array.length() - len + i);
        }
        return result;
    }

    private static Map<String,int[]> getTimeSeriesData(String bmDataSvc,
                                                    Collection<String> ops) throws IOException {
        JSONObject msg = new JSONObject();
        msg.put("operations", new JSONArray(ops));

        JSONObject resp = HttpUtils.doPost(bmDataSvc + "/ts", msg);
        Map<String,int[]> data = new HashMap<String,int[]>();
        Iterator keys = resp.keys();
        while (keys.hasNext()) {
            String k = (String) keys.next();
            JSONArray array = resp.getJSONArray(k);
            int[] ts = new int[array.length()];
            for (int i = 0; i < ts.length; i++) {
                ts[i] = array.getInt(i);
            }
            data.put(k, ts);
        }
        return data;
    }

    private static void printTitle(String text, char underline) {
        System.out.println("\n" + text);
        for (int i = 0; i < text.length(); i++) {
            System.out.print(underline);
        }
        System.out.println();
    }

    private static class TraceAnalysisResult {
        // run QBETS on individual time series and sum the results
        private int approach1;

        // aggregate the time series and run QBETS
        private int approach2;

        // current data point in the aggregate time series
        private int sum;
    }
}
