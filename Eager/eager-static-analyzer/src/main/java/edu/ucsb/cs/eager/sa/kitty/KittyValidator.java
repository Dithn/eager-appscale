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

import edu.ucsb.cs.eager.sa.kitty.qbets.TimeSeries;
import edu.ucsb.cs.eager.sa.kitty.qbets.TraceAnalysisResult;
import org.apache.commons.cli.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;

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
        TimeSeries benchmarkValues = parseBenchmarkFile(benchmarkFile);
        // Pull data from 1 day back at most. Otherwise the analysis is going to take forever.
        long start = benchmarkValues.getTimestampByIndex(0) - 3600 * 24 * 1000;
        long end = benchmarkValues.getTimestampByIndex(benchmarkValues.length() - 1);
        config.setStart(start);
        config.setEnd(end);
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

        kitty.run(config, methods);
        TraceAnalysisResult[] result = kitty.getSummary(method).findLargest();
        int startIndex = findClosestIndex(benchmarkValues.getTimestampByIndex(0), result);
        if (startIndex < 0) {
            System.err.println("Prediction time-line do not overlap with benchmark time-line.");
            return;
        }

        System.out.println("[validate] timestamp prediction 1st 3c 5p");
        for (int i = startIndex; i < result.length; i+=15) {
            SLAViolationInfo vi = findSLAViolations(result, i, benchmarkValues);
            TraceAnalysisResult r = result[i];
            System.out.printf("[validate] %d %5d %-8s %-8s %-8s\n", r.getTimestamp(), r.getApproach2(),
                    getTime(r.getTimestamp(), vi.firstViolation),
                    getTime(r.getTimestamp(), vi.first3CViolations),
                    getTime(r.getTimestamp(), vi.first5PViolations));
        }
    }

    private int findClosestIndex(long ts, TraceAnalysisResult[] result) {
        for (int i = 0; i < result.length; i++) {
            if (result[i].getTimestamp() >= ts) {
                return i;
            }
        }
        return -1;
    }

    private String getTime(long start, long end) {
        if (end < 0) {
            return "N/A";
        }
        double duration = (end - start) / 1000.0;
        return String.format("%.2f", duration);
    }

    private SLAViolationInfo findSLAViolations(TraceAnalysisResult[] results, int index,
                                               TimeSeries samples) {
        TraceAnalysisResult r = results[index];
        long ts = r.getTimestamp();
        int prediction = r.getApproach2();
        int cwrong = r.getCwrong();
        int consecutiveViolations = 0, consecutiveSamples = 0, total = 0, totalViolations = 0;
        SLAViolationInfo vi = new SLAViolationInfo();
        for (int i = 0; i < samples.length(); i++) {
            long time = samples.getTimestampByIndex(i);
            if (time < ts) {
                continue;
            }

            int value = samples.getValueByIndex(i);
            total++;
            consecutiveSamples++;

            if (value > prediction) {
                totalViolations++;
                consecutiveViolations++;
                if (vi.firstViolation < 0) {
                    vi.firstViolation = time;
                }
                if (consecutiveViolations == cwrong && vi.first3CViolations < 0) {
                    vi.first3CViolations = time;
                }
                double percentage = ((double) totalViolations) / total;
                if (percentage > 0.05 && vi.first5PViolations < 0 && total > 100) {
                    vi.first5PViolations = time;
                }
            } else {
                consecutiveViolations = 0;
            }

            if (consecutiveSamples == cwrong) {
                consecutiveSamples = 0;
                consecutiveViolations = 0;
                if (index + total < results.length) {
                    cwrong = results[index + total].getCwrong();
                } else {
                    cwrong = results[results.length - 1].getCwrong();
                }
            }

            if (vi.firstViolation > 0 && vi.first3CViolations > 0 && vi.first5PViolations > 0) {
                break;
            }
        }
        return vi;
    }

    private TimeSeries parseBenchmarkFile(String path) throws IOException {
        TimeSeries timeSeries = new TimeSeries();
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] segments = line.split("\\s+");
            timeSeries.add(Long.parseLong(segments[0]), Integer.parseInt(segments[1]));
        }
        reader.close();
        return timeSeries;
    }

    private static class SLAViolationInfo {
        long firstViolation = -1L;
        long first3CViolations = -1L;
        long first5PViolations = -1L;
    }

}
