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

    public static Map<MethodInfo,TraceAnalysisResult[]> predict(PredictionConfig config,
                               Collection<MethodInfo> methods) throws IOException {
        QBETSTracingPredictor predictor = new QBETSTracingPredictor(config, methods);
        return predictor.run();
    }

    private TimeSeriesDataCache cache;
    private PredictionConfig config;
    private Collection<MethodInfo> methods;

    public QBETSTracingPredictor(PredictionConfig config, Collection<MethodInfo> methods) {
        this.config = config;
        this.methods = methods;
    }

    public Map<MethodInfo,TraceAnalysisResult[]> run() throws IOException {
        Set<String> ops = new HashSet<String>();
        for (MethodInfo m : methods) {
            if (config.isEnabledMethod(m.getName())) {
                for (Path path : m.getPaths()) {
                    for (APICall call : path.calls()) {
                        ops.add(call.getId());
                    }
                }
            }
        }

        if (ops.isEmpty()) {
            println("No methods with API calls found.");
            return null;
        }

        println("\nRetrieving time series data for " + ops.size() + " API " +
                PredictionUtils.pluralize(ops.size(), "call") + "...");
        cache = new TimeSeriesDataCache(getTimeSeriesData(ops, config));
        println("Retrieved time series length: " + cache.getTimeSeriesLength());
        if (config.getStart() != -1 || config.getEnd() != -1) {
            println("Time range: " + config.getStart() + " - " + config.getEnd());
        }

        Map<MethodInfo,TraceAnalysisResult[]> results = new HashMap<MethodInfo, TraceAnalysisResult[]>();
        for (MethodInfo m : methods) {
            if (config.isEnabledMethod(m.getName())) {
                TraceAnalysisResult[] methodResults = analyzeMethod(m);
                if (methodResults != null) {
                    results.put(m, methodResults);
                }
            }
        }
        return results;
    }

    private TraceAnalysisResult[] analyzeMethod(MethodInfo method) throws IOException {
        printTitle(method.getName(), '=');
        List<Path> pathsOfInterest = PredictionUtils.getPathsOfInterest(method);
        if (pathsOfInterest.size() == 0) {
            println("No paths with API calls found.");
            return null;
        }

        println(pathsOfInterest.size() + PredictionUtils.pluralize(
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

        println(uniquePaths.size() + " unique " + PredictionUtils.pluralize(
                uniquePaths.size(), "path") + " with API calls found.");
        List<TraceAnalysisResult> results = new ArrayList<TraceAnalysisResult>();
        for (int i = 0; i < uniquePaths.size(); i++) {
            results.add(analyzePath(method, uniquePaths.get(i), i));
        }
        return results.toArray(new TraceAnalysisResult[results.size()]);
    }

    private TraceAnalysisResult analyzePath(MethodInfo method, Path path, int pathIndex) throws IOException {
        printTitle("Path: " + pathIndex, '-');
        println("API Calls: " + path);

        int tsLength = cache.getTimeSeriesLength();
        int pathLength = path.size();
        double adjustedQuantile = Math.pow(config.getQuantile(), 1.0/pathLength);

        int minIndex = (int) (Math.log(config.getConfidence()) / Math.log(adjustedQuantile)) + 10;
        println("Minimum acceptable time series length: " + (minIndex + 1));

        int dataPoints = tsLength - minIndex;
        if (dataPoints <= 0) {
            println("Insufficient data in time series...");
            return null;
        }
        println("Number of data points analyzed: " + dataPoints);

        int[] actualSums = new int[dataPoints];
        for (int i = 0; i < dataPoints; i++) {
            for (APICall call : path.calls()) {
                // Sum up the actual values from the original time series
                actualSums[i] += cache.getTimeSeries(call)[i + minIndex];
            }
        }

        int[] quantileSums = approach1(path, adjustedQuantile, dataPoints);
        int[] aggregateQuantiles = approach2(path, tsLength, dataPoints);

        TraceAnalysisResult[] results = new TraceAnalysisResult[dataPoints];
        for (int i = 0; i < dataPoints; i++) {
            results[i] = new TraceAnalysisResult();
            results[i].approach1 = quantileSums[i];
            results[i].approach2 = aggregateQuantiles[i];
            results[i].sum = actualSums[i];
        }

        println("");
        int failures = 0;
        println("[trace][method][path] index p1 p2 current  success success_rate");
        for (int i = 0; i < results.length; i++) {
            TraceAnalysisResult r = results[i];
            if (i > 0) {
                boolean success = r.sum < results[i - 1].approach2;
                if (!success) {
                    failures++;
                }
                double successRate = ((double)(i - failures) / i) * 100.0;
                println(String.format("[trace][%s][%d] %4d %4d %4d %4d  %-5s %4.4f",
                        method.getName(), pathIndex, i + minIndex, r.approach1, r.approach2,
                        r.sum, success, successRate));
            } else {
                println(String.format("[trace][%s][%d] %4d %4d %4d %4d  %-5s %-7s",
                        method.getName(), pathIndex, i + minIndex, r.approach1, r.approach2,
                        r.sum, "N/A", "N/A"));
            }
        }

        return results[results.length - 1];
    }

    private int[] approach1(Path path, double adjustedQuantile, int dataPoints) throws IOException {
        for (APICall call : path.calls()) {
            if (!cache.containsQuantiles(call, path.size())) {
                println("Calculating quantiles for: " + call.getId() +
                        " (q = " + adjustedQuantile + "; c = " + config.getConfidence() + ")");
                int[] quantilePredictions = getQuantilePredictions(cache.getTimeSeries(call),
                        call.getId(), dataPoints, adjustedQuantile);
                cache.putQuantiles(call, path.size(), quantilePredictions);
            }
        }

        int[] quantileSums = new int[dataPoints];
        for (int i = 0; i < dataPoints; i++) {
            for (APICall call : path.calls()) {
                // Sum up the adjusted quantiles
                quantileSums[i] += cache.getQuantiles(call, path.size())[i];
            }
        }
        return quantileSums;
    }

    private int[] approach2(Path path, int tsLength, int dataPoints) throws IOException {
        // create new aggregate time series
        int[] aggregate = new int[tsLength];
        for (int i = 0; i < tsLength; i++) {
            for (APICall call : path.calls()) {
                aggregate[i] += cache.getTimeSeries(call)[i];
            }
        }

        // Approach 2
        if (!cache.containsQuantiles(path, path.size())) {
            int[] quantilePredictions = getQuantilePredictions(aggregate, path.getId(),
                    dataPoints, config.getQuantile());
            cache.putQuantiles(path, path.size(), quantilePredictions);
        }
        return cache.getQuantiles(path, path.size());
    }

    /**
     * Obtain a trace of quantiles for a time series
     *
     * @param ts Time series to be analyzed
     * @param name A human readable name for the time series
     * @param len Length of the quantile trace to be returned
     * @param quantile Quantile to be calculated (e.g. 0.95)
     * @return An array of quantiles
     * @throws IOException on error
     */
    private int[] getQuantilePredictions(int[] ts, String name, int len,
                                      double quantile) throws IOException {
        JSONObject msg = new JSONObject();
        msg.put("quantile", quantile);
        msg.put("confidence", config.getConfidence());
        msg.put("data", new JSONArray(ts));
        msg.put("name", name);

        JSONObject resp = HttpUtils.doPost(config.getBenchmarkDataSvc() + "/cpredict", msg);
        JSONArray array = resp.getJSONArray("Predictions");
        int[] result = new int[len];
        // Just return the last len elements of the resulting array
        for (int i = 0; i < len; i++) {
            result[i] = array.getInt(array.length() - len + i);
        }
        return result;
    }

    private Map<String,int[]> getTimeSeriesData(Collection<String> ops,
                                                PredictionConfig config) throws IOException {
        JSONObject msg = new JSONObject();
        msg.put("operations", new JSONArray(ops));
        if (config.getStart() > 0) {
            msg.put("start", config.getStart());
        }
        if (config.getEnd() > 0) {
            msg.put("end", config.getEnd());
        }

        JSONObject resp = HttpUtils.doPost(config.getBenchmarkDataSvc() + "/ts", msg);
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

        if (data.size() == 0) {
            throw new IllegalStateException("No time series data found");
        }
        return data;
    }

    private void println(String msg) {
        if (!config.isHideOutput()) {
            System.out.println(msg);
        }
    }

    private void printTitle(String text, char underline) {
        println("\n" + text);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            builder.append(underline);
        }
        println(builder.toString());
    }

}
