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

import java.io.IOException;

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
        TimeSeries benchmarkValues = PredictionUtils.parseBenchmarkFile(benchmarkFile);
        // Pull data from 1 day back at most. Otherwise the analysis is going to take forever.
        long start = benchmarkValues.getTimestampByIndex(0) - 3600 * 24 * 1000;
        long end = benchmarkValues.getTimestampByIndex(benchmarkValues.length() - 1);

        TraceAnalysisResult[] result = PredictionUtils.makePredictions(config, start, end);
        if (result == null) {
            System.err.println("Failed to find the method: " + config.getMethods()[0]);
            return;
        }
        System.out.println();

        int startIndex = PredictionUtils.findClosestIndex(benchmarkValues.getTimestampByIndex(0), result);
        if (startIndex < 0) {
            System.err.println("Prediction time-line do not overlap with benchmark time-line.");
            return;
        }

        System.out.println("[validate] timestamp prediction 1st 3c 5p");
        for (int i = startIndex; i < result.length; i+=15) {
            SLAViolationInfo vi = findSLAViolations(result, i, benchmarkValues);
            TraceAnalysisResult r = result[i];
            System.out.printf("[validate] %d %5d %-8s %-8s %-8s\n", r.getTimestamp(), r.getApproach2(),
                    PredictionUtils.getTime(r.getTimestamp(), vi.firstViolation),
                    PredictionUtils.getTime(r.getTimestamp(), vi.first3CViolations),
                    PredictionUtils.getTime(r.getTimestamp(), vi.first5PViolations));
        }
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

    private static class SLAViolationInfo {
        long firstViolation = -1L;
        long first3CViolations = -1L;
        long first5PViolations = -1L;
    }

}
