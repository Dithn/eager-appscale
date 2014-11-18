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
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class QBETSTracingPredictor {

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

        TimeSeriesDataCache cache = new TimeSeriesDataCache(
                getTimeSeriesData(config.getBenchmarkDataSvc(), ops));
        if (cache.getTimeSeriesLength() < 200) {
            System.out.println("In sufficient data in time series.");
            return;
        }

        for (MethodInfo m : methods) {
            analyzeMethod(m, cache, config);
        }
    }

    private static void analyzeMethod(MethodInfo method, TimeSeriesDataCache cache,
                                      PredictionConfig config) throws IOException {
        Collection<List<APICall>> pathsOfInterest = PredictionUtils.getPathsOfInterest(method);
        if (pathsOfInterest.size() == 0) {
            return;
        }

        // TODO: Compute the list of 'unique' paths
        for (List<APICall> path : pathsOfInterest) {
            analyzePath(path, cache, config);
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

        TraceAnalysisResult[] results = new TraceAnalysisResult[tsLength - 199];
        int failures = 0;
        System.out.println("\nPos Prediction1 Prediction2 CurrentSum Success SuccessRate");
        for (int tsPos = 199; tsPos < tsLength; tsPos++) {
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
            results[tsPos - 199] = r;

            if (tsPos > 199) {
                boolean success = r.sum < results[tsPos - 199 - 1].approach2;
                if (!success) {
                    failures++;
                }
                double successRate = ((double)(tsPos - 199 - failures) / (tsPos - 199)) * 100.0;
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

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        JSONObject svcResponse;
        try {
            HttpPost request = new HttpPost(bmDataSvc + "/cpredict");
            StringEntity params = new StringEntity(msg.toString());
            request.addHeader("content-type", "application/json");
            request.setEntity(params);

            HttpResponse response = httpClient.execute(request);
            InputStream in = response.getEntity().getContent();
            StringBuilder sb = new StringBuilder();
            byte[] data = new byte[128];
            int len;
            while ((len = in.read(data)) != -1) {
                sb.append(new String(data, 0, len));
            }
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException(sb.toString());
            }
            svcResponse = new JSONObject(sb.toString());
            return svcResponse.getInt("Prediction");
        } finally {
            httpClient.close();
        }
    }

    private static Map<String,int[]> getTimeSeriesData(String bmDataSvc,
                                                    Collection<String> ops) throws IOException {
        JSONObject msg = new JSONObject();
        msg.put("operations", new JSONArray(ops));

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        JSONObject svcResponse;
        try {
            HttpPost request = new HttpPost(bmDataSvc + "/ts");
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

        Map<String,int[]> data = new HashMap<String,int[]>();
        Iterator keys = svcResponse.keys();
        while (keys.hasNext()) {
            String k = (String) keys.next();
            JSONArray array = svcResponse.getJSONArray(k);
            int[] ts = new int[array.length()];
            for (int i = 0; i < ts.length; i++) {
                ts[i] = array.getInt(i);
            }
            data.put(k, ts);
        }
        return data;
    }

    private static class TraceAnalysisResult {
        private int approach1;
        private int approach2;
        private int sum;
    }
}
