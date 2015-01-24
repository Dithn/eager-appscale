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
        Kitty kitty = new Kitty();
        Collection<MethodInfo> methods = kitty.getMethods(config);

        Map<Long,Integer> benchmarkValues = parseBenchmarkFile(benchmarkFile);
        for (long ts : benchmarkValues.keySet()) {
            TimestampInfo info = getTimestampInfo(config, ts);
            if (info.count <= 1000) {
                System.out.println("Not enough data in Watchtower for validation.");
                continue;
            }
            System.out.println("Making predictions up to: " + info.timestamp);
            config.setStart(-1L);
            config.setEnd(info.timestamp);
            kitty.run(config, methods);
            Map<MethodInfo,TraceAnalysisResult[]> summary = kitty.getSummary();
            for (MethodInfo m : summary.keySet()) {
                TraceAnalysisResult[] results = summary.get(m);
                System.out.println("\nPerforming validation for method: " + m.getName());
                findSLAViolations(info.timestamp, findMax(results), benchmarkValues);
            }

            break;
        }
    }

    private void findSLAViolations(long ts, int prediction, Map<Long,Integer> samples) {
        System.out.println("Prediction at " + ts + ": " + prediction);
        long firstViolation = -1L;
        long first3CViolations = -1L;
        long first5PViolations = -1L;
        int consecutiveViolations = 0;
        int total = 0;
        int totalViolations = 0;
        for (Map.Entry<Long,Integer> entry : samples.entrySet()) {
            total++;
            if (entry.getValue() > prediction) {
                totalViolations++;
                if (firstViolation < 0) {
                    firstViolation = entry.getKey();
                    printTime("First violation", ts, firstViolation);
                }
                consecutiveViolations++;
                if (consecutiveViolations == 3 && first3CViolations < 0) {
                    first3CViolations = entry.getKey();
                    printTime("First 3C violations", ts, first3CViolations);
                }
                double percentage = ((double) totalViolations) / total;
                if (percentage > 0.05 && first5PViolations < 0) {
                    first5PViolations = entry.getKey();
                    printTime("More than 5% violations", ts, first5PViolations);
                }
            } else {
                consecutiveViolations = 0;
            }

            if (firstViolation > 0 && first3CViolations > 0 && first5PViolations > 0) {
                break;
            }
        }
    }

    private void printTime(String title, long start, long ts) {
        double duration = (ts - start)/1000.0;
        System.out.println(title + " at: " + ts + "[" + duration + " seconds]");
    }

    private int findMax(TraceAnalysisResult[] results) {
        int max = -1;
        for (TraceAnalysisResult r : results) {
            if (r.getApproach2() > max) {
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

}
