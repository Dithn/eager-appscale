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

package edu.ucsb.cs.eager.sa.kitty;

import edu.ucsb.cs.eager.sa.kitty.qbets.HttpUtils;
import edu.ucsb.cs.eager.sa.kitty.qbets.TraceAnalysisResult;
import org.apache.commons.cli.*;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class KittyValidator {

    public static void main(String[] args) throws IOException {
        Options options = Kitty.getOptions();
        PredictionUtils.addOption(options, "bf", "benchmark-file", true,
                "File containing benchmark data");
        CommandLine cmd = PredictionUtils.parseCommandLineArgs(options, args, "KittyValidator");
        PredictionConfig config = Kitty.getPredictionConfig(cmd);
        if (config == null) {
            return;
        } else if (config.getMethods() == null || config.getMethods().length != 1) {
            System.err.println("one method must be specified for analysis.");
            return;
        }

        String benchmarkFile = cmd.getOptionValue("bf");
        if (benchmarkFile == null) {
            System.err.println("benchmark file must be specified.");
            return;
        }
        KittyValidator validator = new KittyValidator();
        validator.run(config, benchmarkFile);
    }

    public void run(PredictionConfig config, String benchmarkFile) throws IOException {
        config.setStart(-1L);
        config.setHideOutput(true);

        Kitty kitty = new Kitty();
        Collection<MethodInfo> methods = kitty.getMethods(config);
        MethodInfo method = null;
        for (MethodInfo m : methods) {
            if (config.isEnabledMethod(m.getName())) {
                method = m;
                break;
            }
        }
        if (method == null) {
            System.err.println("Failed to find the method: " + config.getMethods()[0]);
            return;
        }
        System.out.println();

        long last = 0;
        Map<Long,Integer> benchmarkValues = parseBenchmarkFile(benchmarkFile);
        System.out.println("[validate] timestamp prediction 1st 3c 5p");
        for (long ts : benchmarkValues.keySet()) {
            if (ts - last < 15 * 60 * 1000) {
                // Jump ahead in approximately 15 min intervals
                continue;
            }
            last = ts;

            TimestampInfo info = getTimestampInfo(config, ts);
            if (info.count < 1000) {
                // We mandate that Watchtower at least have 1000 data points
                // for this analysis to be useful.
                System.out.println("Not enough data in Watchtower for validation.");
                continue;
            }

            config.setEnd(info.timestamp);
            kitty.run(config, methods);
            int max = findMax(kitty.getSummary(method));
            SLAViolationInfo vi = findSLAViolations(info.timestamp, max, benchmarkValues);
            System.out.printf("[validate] %d %5d %-8s %-8s %-8s\n", info.timestamp, max,
                    getTime(info.timestamp, vi.firstViolation),
                    getTime(info.timestamp, vi.first3CViolations),
                    getTime(info.timestamp, vi.first5PViolations));
        }
    }

    private String getTime(long start, long end) {
        if (end < 0) {
            return "N/A";
        }
        double duration = (end - start) / 1000.0;
        return String.format("%.2f", duration);
    }

    private SLAViolationInfo findSLAViolations(long ts, int prediction, Map<Long,Integer> samples) {
        int consecutiveViolations = 0, total = 0, totalViolations = 0;
        SLAViolationInfo vi = new SLAViolationInfo();
        for (Map.Entry<Long,Integer> entry : samples.entrySet()) {
            if (entry.getKey() < ts) {
                continue;
            }
            total++;
            if (entry.getValue() > prediction) {
                totalViolations++;
                consecutiveViolations++;
                if (vi.firstViolation < 0) {
                    vi.firstViolation = entry.getKey();
                }
                if (consecutiveViolations == 3 && vi.first3CViolations < 0) {
                    vi.first3CViolations = entry.getKey();
                }
                double percentage = ((double) totalViolations) / total;
                if (percentage > 0.05 && vi.first5PViolations < 0) {
                    vi.first5PViolations = entry.getKey();
                }
            } else {
                consecutiveViolations = 0;
            }

            if (vi.firstViolation > 0 && vi.first3CViolations > 0 && vi.first5PViolations > 0) {
                break;
            }
        }
        return vi;
    }

    private int findMax(TraceAnalysisResult[] results) {
        int max = -1;
        for (TraceAnalysisResult r : results) {
            if (r.getApproach2() > max) {
                // Only use the 2nd prediction (less conservative) for this analysis.
                max = r.getApproach2();
            }
        }
        return max;
    }

    private Map<Long,Integer> parseBenchmarkFile(String path) throws IOException {
        Map<Long,Integer> benchmarkValues = new TreeMap<Long, Integer>();
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] segments = line.split(" ");
            benchmarkValues.put(Long.parseLong(segments[0]), Integer.parseInt(segments[1]));
        }
        reader.close();
        return benchmarkValues;
    }

    private TimestampInfo getTimestampInfo(PredictionConfig config, long limit) throws IOException {
        String url = config.getBenchmarkDataSvc() + "/tsinfo?limit=" + limit;
        JSONObject resp = HttpUtils.doGet(url);
        TimestampInfo info = new TimestampInfo();
        info.count = resp.getLong("Count");
        info.timestamp = resp.getLong("Latest");
        return info;
    }

    private static class TimestampInfo {
        long count;
        long timestamp;
    }

    private static class SLAViolationInfo {
        long firstViolation = -1L;
        long first3CViolations = -1L;
        long first5PViolations = -1L;
    }

}
