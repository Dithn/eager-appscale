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
import java.util.concurrent.*;

/**
 * QBETSTracingPredictor obtains time series data for different API calls,
 * and performs an offline trace on them. For each entry in a time series,
 * it calculates 2 quantile predictions, and computes how often the actual
 * values fall within the predicted bounds.
 */
public class QBETSTracingPredictor implements Predictor {

    private TimeSeriesDataCache cache;
    private QBETSConfig config;
    private CacheUpdater cacheUpdater;
    private ExecutorService methodWorkerPool;

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

        methodWorkerPool = Executors.newCachedThreadPool();
        cacheUpdater = new CacheUpdater(cache, config);

        try {
            final QBETSTracingPredictionOutput output = new QBETSTracingPredictionOutput();
            Map<MethodInfo,Future<TraceAnalysisResultSet>> methodFutures = new HashMap<>();
            for (final MethodInfo m : methods) {
                Callable<TraceAnalysisResultSet> methodWorker = new Callable<TraceAnalysisResultSet>() {
                    @Override
                    public TraceAnalysisResultSet call() throws Exception {
                        return analyzeMethod(m);
                    }
                };
                methodFutures.put(m, methodWorkerPool.submit(methodWorker));
            }

            for (MethodInfo m : methods) {
                try {
                    TraceAnalysisResultSet r = methodFutures.get(m).get();
                    if (r != null) {
                        output.add(m, r);
                    }
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
            return output;

        } finally {
            methodWorkerPool.shutdownNow();
            cacheUpdater.close();
        }
    }

    private TraceAnalysisResultSet analyzeMethod(MethodInfo method) throws IOException {
        List<Path> pathsOfInterest = PredictionUtils.getPathsOfInterest(method);
        if (pathsOfInterest.size() == 0) {
            log(method, -1, "No paths with API calls found.");
            return null;
        }

        log(method, -1, pathsOfInterest.size() + PredictionUtils.pluralize(
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

        log(method, -1, uniquePaths.size() + " unique " + PredictionUtils.pluralize(
                uniquePaths.size(), "path") + " with API calls found.");
        TraceAnalysisResultSet resultSet = new TraceAnalysisResultSet();
        for (int i = 0; i < uniquePaths.size(); i++) {
            resultSet.addResult(analyzePath(method, uniquePaths.get(i), i));
        }
        return resultSet;
    }

    private PathResult analyzePath(final MethodInfo method, final Path path,
                                   final int pathIndex) throws IOException {
        int tsLength = cache.getTimeSeriesLength();
        int pathLength = path.size();
        final double adjustedQuantile = Math.pow(config.getQuantile(), 1.0/pathLength);

        int minIndex = (int) (Math.log(config.getConfidence()) / Math.log(adjustedQuantile)) + 10;
        log(method, pathIndex, "Minimum acceptable time series length: " + (minIndex + 1));

        final int dataPoints = tsLength - minIndex;
        if (dataPoints <= 0) {
            log(method, pathIndex, "Insufficient data in time series...");
            return null;
        }
        log(method, pathIndex, "Number of data points analyzed: " + dataPoints);

        final TimeSeries actualSums = getAggregateTimeSeries(path.calls());
        TimeSeries quantileSums, aggregateQuantiles;
        if (path.size() > 1) {
            Future<TimeSeries> approach1 = methodWorkerPool.submit(new Callable<TimeSeries>() {
                @Override
                public TimeSeries call() throws Exception {
                    return approach1(method, path, pathIndex, adjustedQuantile, dataPoints);
                }
            });
            Future<TimeSeries> approach2 = methodWorkerPool.submit(new Callable<TimeSeries>() {
                @Override
                public TimeSeries call() throws Exception {
                    return approach2(method, path, pathIndex, actualSums, dataPoints);
                }
            });
            try {
                quantileSums = approach1.get();
                aggregateQuantiles = approach2.get();
            } catch (Exception e) {
                throw new IOException(e);
            }
        } else {
            quantileSums = approach1(method, path, pathIndex, adjustedQuantile, dataPoints);
            aggregateQuantiles = approach2(method, path, pathIndex, actualSums, dataPoints);
        }


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

    private TimeSeries approach1(MethodInfo method, Path path, int pathIndex,
                                 double adjustedQuantile, int dataPoints) throws IOException {
        if (config.isDisableApproach1()) {
            return null;
        }
        cacheUpdater.updateCacheForAPICalls(path, method, pathIndex, adjustedQuantile, dataPoints);

        // Sum up the adjusted quantiles
        List<TimeSeries> ts = new ArrayList<>();
        for (APICall call : path.calls()) {
            ts.add(cache.getQuantiles(call, path.size()));
        }
        return aggregate(ts);
    }

    private TimeSeries approach2(MethodInfo method, Path path, int pathIndex,
                                 TimeSeries aggr, int dataPoints) throws IOException {
        // Approach 2
        cacheUpdater.updateCacheForPath(path, method, pathIndex, config.getQuantile(),
                dataPoints, aggr);
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

    private void log(MethodInfo method, int pathIndex, String msg) {
        String thread = Thread.currentThread().getName();
        if (pathIndex < 0) {
            System.out.printf("log: {%s}{%s}{-} %s\n", thread, method.getName(), msg);
        } else {
            System.out.printf("log: {%s}{%s}{%d} %s\n", thread, method.getName(), pathIndex, msg);
        }
    }
}
