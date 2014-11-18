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
import edu.ucsb.cs.eager.sa.kitty.PredictionConfig;
import edu.ucsb.cs.eager.sa.kitty.PredictionUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

public class QBETSTracingPredictor {

    private static final int MIN_INDEX = 199;

    public static void predict(PredictionConfig config,
                               Collection<MethodInfo> methods) throws IOException {

        Set<String> ops = new HashSet<String>();
        for (MethodInfo m : methods) {
            for (List<APICall> path : m.getPaths()) {
                for (APICall call : path) {
                    ops.add(call.getShortName());
                }
            }
        }

        if (ops.isEmpty()) {
            System.out.println("No methods with API calls found.");
            return;
        }

        System.out.println("\nRetrieving time series data for " + ops.size() + " API calls...");
        TimeSeriesDataCache cache = new TimeSeriesDataCache(
                getTimeSeriesData(config.getBenchmarkDataSvc(), ops));
        int timeSeriesLength = cache.getTimeSeriesLength();
        if (timeSeriesLength < MIN_INDEX + 1) {
            System.out.println("In sufficient data in time series.");
            return;
        }
        System.out.println("Retrieved time series length: " + timeSeriesLength);

        for (MethodInfo m : methods) {
            analyzeMethod(m, cache, config);
        }
    }

    private static void analyzeMethod(MethodInfo method, TimeSeriesDataCache cache,
                                      PredictionConfig config) throws IOException {
        printTitle(method.getName(), '=');
        List<List<APICall>> pathsOfInterest = PredictionUtils.getPathsOfInterest(method);
        if (pathsOfInterest.size() == 0) {
            System.out.println("No paths with API calls found.");
            return;
        }

        System.out.println(pathsOfInterest.size() + " paths with API calls found.");

        // TODO: Compute the list of 'unique' paths
        for (int i = 0; i < pathsOfInterest.size(); i++) {
            printTitle("Path: " + i, '-');
            analyzePath(pathsOfInterest.get(i), cache, config);
        }
    }

    private static void analyzePath(List<APICall> path, TimeSeriesDataCache cache,
                                    PredictionConfig config) throws IOException {
        int tsLength = cache.getTimeSeriesLength();
        int pathLength = path.size();
        double adjustedQuantile = Math.pow(config.getQuantile(), 1.0/pathLength);

        // create new aggregate time series
        int[] aggregate = new int[tsLength];
        for (int i = 0; i < tsLength; i++) {
            for (APICall call : path) {
                aggregate[i] += cache.getTimeSeries(call.getShortName())[i];
            }
        }

        TraceAnalysisResult[] results = new TraceAnalysisResult[tsLength - MIN_INDEX];
        int failures = 0;
        System.out.println("\nPos Prediction1 Prediction2 CurrentSum Success SuccessRate");
        for (int tsPos = MIN_INDEX; tsPos < tsLength; tsPos++) {
            // Approach 1
            int prediction1 = 0;
            for (APICall call : path) {
                String op = call.getShortName();
                if (!cache.containsQuantile(op, pathLength, tsPos)) {
                    int[] copy = new int[tsPos + 1];
                    System.arraycopy(cache.getTimeSeries(op), 0, copy, 0, copy.length);
                    int prediction = getQuantilePrediction(config.getBenchmarkDataSvc(), copy,
                            adjustedQuantile, config.getConfidence());
                    cache.putQuantile(op, pathLength, tsPos, prediction);
                }
                prediction1 += cache.getQuantile(op, pathLength, tsPos);
            }

            // Approach 2
            int[] copy = new int[tsPos + 1];
            System.arraycopy(aggregate, 0, copy, 0, copy.length);
            int prediction2 = getQuantilePrediction(config.getBenchmarkDataSvc(), copy,
                    config.getQuantile(), config.getConfidence());

            TraceAnalysisResult r = new TraceAnalysisResult();
            r.approach1 = prediction1;
            r.approach2 = prediction2;
            r.sum = aggregate[tsPos];
            results[tsPos - MIN_INDEX] = r;

            if (tsPos > MIN_INDEX) {
                boolean success = r.sum < results[tsPos - MIN_INDEX - 1].approach2;
                if (!success) {
                    failures++;
                }
                double successRate = ((double)(tsPos - MIN_INDEX - failures) /
                        (tsPos - MIN_INDEX)) * 100.0;
                System.out.printf("%4d %4dms %4dms %4dms  %-5s %4.4f\n", tsPos, r.approach1,
                        r.approach2, r.sum, success, successRate);
            } else {
                System.out.printf("%4d %4dms %4dms %4dms  %-5s %-7s\n", tsPos, r.approach1,
                        r.approach2, r.sum, "N/A", "N/A");
            }
        }
    }

    private static int getQuantilePrediction(String bmDataSvc, int[] ts,
                                      double quantile, double confidence) throws IOException {
        JSONObject msg = new JSONObject();
        msg.put("quantile", quantile);
        msg.put("confidence", confidence);
        msg.put("data", new JSONArray(ts));

        JSONObject resp = HttpUtils.doPost(bmDataSvc + "/cpredict", msg);
        return resp.getInt("Prediction");
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
        private int approach1;
        private int approach2;
        private int sum;
    }
}
