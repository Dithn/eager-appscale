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
public class QBETSTracingPredictor implements Predictor {

    private TimeSeriesDataCache cache;
    private QBETSConfig config;

    public QBETSTracingPredictor(QBETSConfig config) {
        this.config = config;
    }

    public PredictionOutput run(Collection<MethodInfo> methods) throws IOException {
        Set<String> ops = new HashSet<>();
        for (MethodInfo m : methods) {
            for (Path path : m.getPaths()) {
                for (APICall call : path.calls()) {
                    ops.add(call.getId());
                }
            }
        }

        if (ops.isEmpty()) {
            System.out.println("No methods with API calls found.");
            return null;
        }

        System.out.println("\nRetrieving time series data for " + ops.size() + " API " +
                PredictionUtils.pluralize(ops.size(), "call") + "...");
        cache = new TimeSeriesDataCache(getTimeSeriesData(ops, config));
        System.out.println("Retrieved time series length: " + cache.getTimeSeriesLength());
        if (config.getStart() != -1 || config.getEnd() != -1) {
            System.out.println("Time range: " + config.getStart() + " - " + config.getEnd());
        }

        QBETSTracingPredictionOutput output = new QBETSTracingPredictionOutput();
        for (MethodInfo m : methods) {
            TraceAnalysisResultSet methodResults = analyzeMethod(m);
            if (methodResults != null) {
                output.add(m, methodResults);
            }
        }
        return output;
    }

    private TraceAnalysisResultSet analyzeMethod(MethodInfo method) throws IOException {
        List<Path> pathsOfInterest = PredictionUtils.getPathsOfInterest(method);
        if (pathsOfInterest.size() == 0) {
            System.out.println("No paths with API calls found.");
            return null;
        }

        System.out.println(pathsOfInterest.size() + PredictionUtils.pluralize(
                pathsOfInterest.size(), " path") + " with API calls found.");

        List<Path> uniquePaths = new ArrayList<>();
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
        TraceAnalysisResultSet resultSet = new TraceAnalysisResultSet();
        for (Path uniquePath : uniquePaths) {
            resultSet.addResult(analyzePath(uniquePath));
        }
        return resultSet;
    }

    private PathResult analyzePath(Path path) throws IOException {
        int tsLength = cache.getTimeSeriesLength();
        int pathLength = path.size();
        double adjustedQuantile = Math.pow(config.getQuantile(), 1.0/pathLength);

        int minIndex = (int) (Math.log(config.getConfidence()) / Math.log(adjustedQuantile)) + 10;
        System.out.println("Minimum acceptable time series length: " + (minIndex + 1));

        int dataPoints = tsLength - minIndex;
        if (dataPoints <= 0) {
            System.out.println("Insufficient data in time series...");
            return null;
        }
        System.out.println("Number of data points analyzed: " + dataPoints);

        TimeSeries actualSums = getAggregateTimeSeries(path.calls());
        TimeSeries quantileSums = approach1(path, adjustedQuantile, dataPoints);
        TimeSeries aggregateQuantiles = approach2(path, actualSums, dataPoints);

        TraceAnalysisResult[] results = new TraceAnalysisResult[dataPoints];
        for (int i = 0; i < dataPoints; i++) {
            long ts = aggregateQuantiles.getTimestampByIndex(i);
            TraceAnalysisResult r = new TraceAnalysisResult();
            r.timestamp = ts;
            if (quantileSums != null) {
                r.approach1 = quantileSums.getByTimestamp(ts);
            } else {
                r.approach1 = 0;
            }
            r.approach2 = aggregateQuantiles.getByTimestamp(ts);
            r.cwrong = aggregateQuantiles.getCwrongByTimestamp(ts);
            r.sum = actualSums.getByTimestamp(ts);
            results[i] = r;
        }

        return new PathResult(path, minIndex, results);
    }

    private TimeSeries approach1(Path path, double adjustedQuantile,
                                 int dataPoints) throws IOException {
        if (config.isDisableApproach1()) {
            return null;
        }

        // Sum up the adjusted quantiles
        List<TimeSeries> ts = new ArrayList<>();
        for (APICall call : path.calls()) {
            if (!cache.containsQuantiles(call, path.size())) {
                System.out.println("Calculating quantiles for: " + call.getId() +
                        " (q = " + adjustedQuantile + "; c = " + config.getConfidence() + ")");
                TimeSeries quantilePredictions = getQuantilePredictions(cache.getTimeSeries(call),
                        call.getId(), dataPoints, adjustedQuantile);
                cache.putQuantiles(call, path.size(), quantilePredictions);
            }
            ts.add(cache.getQuantiles(call, path.size()));
        }

        return aggregate(ts);
    }

    private TimeSeries approach2(Path path, TimeSeries aggr, int dataPoints) throws IOException {
        // Approach 2
        if (!cache.containsQuantiles(path, path.size())) {
            TimeSeries quantilePredictions = getQuantilePredictions(aggr, path.getId(),
                    dataPoints, config.getQuantile());
            cache.putQuantiles(path, path.size(), quantilePredictions);
        }
        return cache.getQuantiles(path, path.size());
    }

    public TimeSeries getAggregateTimeSeries(List<APICall> ops) {
        List<TimeSeries> ts = new ArrayList<>();
        for (APICall call : ops) {
            ts.add(cache.getTimeSeries(call));
        }
        return aggregate(ts);
    }

    private TimeSeries aggregate(List<TimeSeries> list) {
        TimeSeries aggr = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            aggr = aggr.aggregate(list.get(i));
        }
        return aggr;
    }

    /**
     * Obtain a trace of quantiles for a time series
     *
     * @param ts Time series to be analyzed
     * @param name A human readable name for the time series
     * @param len Length of the quantile trace to be returned
     * @param quantile Quantile to be calculated (e.g. 0.95)
     * @return A time series of predictions
     * @throws IOException on error
     */
    private TimeSeries getQuantilePredictions(TimeSeries ts, String name, int len,
                                      double quantile) throws IOException {
        JSONObject msg = new JSONObject();
        msg.put("quantile", quantile);
        msg.put("confidence", config.getConfidence());
        msg.put("data", ts.toJSON());
        msg.put("name", name);

        JSONObject resp = HttpUtils.doPost(config.getBenchmarkDataSvc() + "/cpredict", msg);
        JSONArray array = resp.getJSONArray("Predictions");
        // Just return the last len elements of the resulting array
        return new TimeSeries(array, len);
    }

    private Map<String,TimeSeries> getTimeSeriesData(Collection<String> ops,
                                                QBETSConfig config) throws IOException {
        JSONObject msg = new JSONObject();
        msg.put("operations", new JSONArray(ops));
        if (config.getStart() > 0) {
            msg.put("start", config.getStart());
        }
        if (config.getEnd() > 0) {
            msg.put("end", config.getEnd());
        }

        JSONObject resp = HttpUtils.doPost(config.getBenchmarkDataSvc() + "/ts", msg);
        Map<String,TimeSeries> data = new HashMap<>();
        Iterator keys = resp.keys();
        while (keys.hasNext()) {
            String k = (String) keys.next();
            JSONArray array = resp.getJSONArray(k);
            data.put(k, new TimeSeries(array, array.length()));
        }

        if (data.size() == 0) {
            throw new IllegalStateException("No time series data found");
        }
        return data;
    }
}
