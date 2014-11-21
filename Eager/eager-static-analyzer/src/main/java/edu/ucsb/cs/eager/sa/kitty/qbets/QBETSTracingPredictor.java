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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * QBETSTracingPredictor obtains time series data for different API calls,
 * and performs an offline trace on them. For each entry in a time series,
 * it calculates 2 quantile predictions, and computes how often the actual
 * values fall within the predicted bounds.
 */
public class QBETSTracingPredictor {

    private static final int MIN_INDEX = 99;

    public static void predict(PredictionConfig config,
                               Collection<MethodInfo> methods) throws IOException {

        Set<String> ops = new HashSet<String>();
        for (MethodInfo m : methods) {
            for (Path path : m.getPaths()) {
                for (APICall call : path.calls()) {
                    ops.add(call.getShortName());
                }
            }
        }

        if (ops.isEmpty()) {
            System.out.println("No methods with API calls found.");
            return;
        }

        System.out.println("\nRetrieving time series data for " + ops.size() + " API " +
                PredictionUtils.pluralize(ops.size(), "call..."));
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
            analyzePath(method, pathsOfInterest.get(i), i, cache, config);
        }
    }

    private static void analyzePath(MethodInfo method, Path path, int pathIndex,
                                    TimeSeriesDataCache cache, PredictionConfig config) throws IOException {
        printTitle("Path: " + pathIndex, '-');

        int tsLength = cache.getTimeSeriesLength();
        int pathLength = path.size();
        double adjustedQuantile = Math.pow(config.getQuantile(), 1.0/pathLength);

        // create new aggregate time series
        int[] aggregate = new int[tsLength];
        for (int i = 0; i < tsLength; i++) {
            for (APICall call : path.calls()) {
                aggregate[i] += cache.getTimeSeries(call.getShortName())[i];
            }
        }

        int dataPoints = tsLength - MIN_INDEX;
        TraceAnalysisResult[] results = new TraceAnalysisResult[dataPoints];
        Future<?>[] futures = new Future<?>[dataPoints];
        ExecutorService exec = Executors.newFixedThreadPool(8);

        for (int tsPos = 0; tsPos < dataPoints; tsPos++) {
            PredictionWorker worker = new PredictionWorker();
            worker.adjustedQuantile = adjustedQuantile;
            worker.cache = cache;
            worker.config = config;
            worker.path = path;
            worker.results = results;
            worker.tsPos = MIN_INDEX + tsPos;
            worker.aggregate = aggregate;

            futures[tsPos] = exec.submit(worker);
        }

        // Wait for the workers to finish
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (Exception e) {
                throw new IOException("Exception in the prediction worker", e);
            }
        }

        exec.shutdownNow();

        System.out.println();
        int failures = 0;
        System.out.printf("[trace][method][path] index p1 p2 current  success success_rate\n");
        for (int i = 0; i < results.length; i++) {
            TraceAnalysisResult r = results[i];
            if (r.e != null) {
                System.err.printf("[trace][%s][%d] %4d ------------ error ------------\n",
                        method.getName(), pathIndex, i + MIN_INDEX);
                continue;
            }
            if (i > 0) {
                boolean success = r.sum < results[i - 1].approach2;
                if (!success) {
                    failures++;
                }
                double successRate = ((double)(i - failures) / i) * 100.0;
                System.out.printf("[trace][%s][%d] %4d %4d %4d %4d  %-5s %4.4f\n",
                        method.getName(), pathIndex, i + MIN_INDEX, r.approach1, r.approach2,
                        r.sum, success, successRate);
            } else {
                System.out.printf("[trace][%s][%d] %4d %4d %4d %4d  %-5s %-7s\n",
                        method.getName(), pathIndex, i + MIN_INDEX, r.approach1, r.approach2,
                        r.sum, "N/A", "N/A");
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
        // run QBETS on individual time series and sum the results
        private int approach1;

        // aggregate the time series and run QBETS
        private int approach2;

        // current data point in the aggregate time series
        private int sum;

        private Exception e;
    }

    private static class PredictionWorker implements Runnable {

        private Path path;
        private TimeSeriesDataCache cache;
        private int tsPos;
        private double adjustedQuantile;
        private PredictionConfig config;
        private int[] aggregate;
        private TraceAnalysisResult[] results;

        @Override
        public void run() {
            int pathLength = path.size();
            String pathId = path.getId();
            TraceAnalysisResult r = new TraceAnalysisResult();
            try {
                // Approach 1
                int prediction1 = 0;
                for (APICall call : path.calls()) {
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
                int prediction2;
                if (pathLength > 1) {
                    if (!cache.containsQuantile(pathId, pathLength, tsPos)) {
                        int[] copy = new int[tsPos + 1];
                        System.arraycopy(aggregate, 0, copy, 0, copy.length);
                        int prediction = getQuantilePrediction(config.getBenchmarkDataSvc(), copy,
                                config.getQuantile(), config.getConfidence());
                        cache.putQuantile(pathId, pathLength, tsPos, prediction);
                    }
                    prediction2 = cache.getQuantile(pathId, pathLength, tsPos);
                } else {
                    // When there's only one API call in the path, the two predictions
                    // are going to be the same.
                    prediction2 = prediction1;
                }

                r.approach1 = prediction1;
                r.approach2 = prediction2;
                r.sum = aggregate[tsPos];
                if (tsPos % 100 == 0) {
                    System.out.println("Computed the predictions for index: " + tsPos);
                }
            } catch (IOException e) {
                r.e = e;
                System.err.println("Error computing the predictions for index: " + tsPos);
            }
            results[tsPos - MIN_INDEX] = r;
        }
    }
}
