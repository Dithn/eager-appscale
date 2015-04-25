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
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KittySLAEvolutionAnalyzer {

    public static void main(String[] args) throws IOException {
        Options options = Kitty.getOptions();
        PredictionUtils.addOption(options, "bf", "benchmark-file", true,
                "File containing benchmark data");
        PredictionUtils.addOption(options, "ai", "adaptive-intervals", false,
                "Enable adaptive interval analysis");
        PredictionUtils.addOption(options, "mv", "max-violation", false,
                "Output only the max SLA violations");
        PredictionUtils.addOption(options, "vt", "violation-threshold", true,
                "Threshold value used to detect SLA violations");
        CommandLine cmd = PredictionUtils.parseCommandLineArgs(options, args,
                "KittySLAEvolutionAnalyzer");
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
        boolean adaptiveIntervals = cmd.hasOption("ai");
        boolean maxViolationOnly = cmd.hasOption("mv");

        double threshold = 0.0;
        String thresholdString = cmd.getOptionValue("vt");
        if (thresholdString != null) {
            threshold = Double.parseDouble(thresholdString);
        }

        KittySLAEvolutionAnalyzer analyzer = new KittySLAEvolutionAnalyzer();
        analyzer.run(config, benchmarkFile, adaptiveIntervals, maxViolationOnly, threshold);
    }

    public void run(PredictionConfig config, String benchmarkFile,
                    boolean adaptiveIntervals, boolean maxViolationOnly,
                    double threshold) throws IOException {
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

        int startIndex = PredictionUtils.findClosestIndex(
                benchmarkValues.getTimestampByIndex(0), result);
        if (startIndex < 0) {
            System.err.println("Prediction time-line do not overlap with benchmark time-line.");
            return;
        }

        if (adaptiveIntervals) {
            adaptiveIntervalAnalysis(benchmarkValues, result, startIndex, maxViolationOnly, threshold);
        } else {
            fixedIntervalAnalysis(benchmarkValues, result, startIndex, threshold);
        }
    }

    private void adaptiveIntervalAnalysis(TimeSeries benchmarkValues, TraceAnalysisResult[] result,
                                          int startIndex, boolean maxViolationOnly, double threshold) {
        System.out.println("[sla][offset] timestamp prediction timeToViolation(h) violationDelta");
        for (int i = 0; i < result.length - 1 - startIndex; i++) {
            int currentIndex = startIndex + i;
            while (currentIndex > 0 && currentIndex < result.length - 1) {
                TraceAnalysisResult currentPrediction = result[currentIndex];
                Violation violation = findViolation(benchmarkValues, result, currentIndex, threshold);
                String violationDiff = maxViolationOnly ? violation.getMaxValueString() :
                        violation.getValueString();
                System.out.printf("[sla] %d %d %5d %-8s %s\n", i,
                        currentPrediction.getTimestamp(),
                        currentPrediction.getApproach2(),
                        PredictionUtils.getTimeInHours(currentPrediction.getTimestamp(),
                                violation.timestamp),
                        violationDiff);
                currentIndex = PredictionUtils.findClosestIndex(violation.timestamp, result);
            }
        }
    }

    private void fixedIntervalAnalysis(TimeSeries benchmarkValues, TraceAnalysisResult[] result,
                                          int startIndex, double threshold) {

        long interval = getInterval(benchmarkValues, result, startIndex, threshold);
        System.out.println("Calculated interval: " + interval/1000.0/3600.0 + " hours\n");

        int currentIndex = startIndex;
        System.out.println("[sla] timestamp prediction timeToViolation(h)");
        while (currentIndex >= 0 && currentIndex < result.length - 1) {
            TraceAnalysisResult currentPrediction = result[currentIndex];
            Violation violation = findViolation(benchmarkValues, result, currentIndex, threshold);
            System.out.printf("[sla] %d %5d %-8s\n", currentPrediction.getTimestamp(),
                    currentPrediction.getApproach2(),
                    PredictionUtils.getTimeInHours(currentPrediction.getTimestamp(),
                            violation.timestamp));
            currentIndex = PredictionUtils.findClosestIndex(
                    currentPrediction.getTimestamp() + interval, result);
        }
    }

    private long getInterval(TimeSeries benchmarkValues, TraceAnalysisResult[] result,
                             int startIndex, double threshold) {
        List<Long> violationTimes = new ArrayList<>();
        for (int i = startIndex; i < result.length; i+=15) {
            Violation violation = findViolation(benchmarkValues, result, i, threshold);
            violationTimes.add(violation.timestamp - result[i].getTimestamp());
        }
        Collections.sort(violationTimes);
        int fifthPercentileIndex = (int) Math.ceil(0.05 * violationTimes.size());
        return violationTimes.get(fifthPercentileIndex);
    }

    private Violation findViolation(TimeSeries benchmarkValues, TraceAnalysisResult[] results,
                                    int index, double threshold) {
        int consecutiveViolations = 0, consecutiveSamples = 0, total = 0;
        TraceAnalysisResult r = results[index];
        long ts = r.getTimestamp();
        int sla = r.getApproach2();
        int cwrong = r.getCwrong();
        List<Integer> violationValues = new ArrayList<>();

        for (int i = 0; i < benchmarkValues.length(); i++) {
            long currentTime = benchmarkValues.getTimestampByIndex(i);
            if (currentTime < ts) {
                continue;
            }

            total++;
            consecutiveSamples++;
            int currentValue = benchmarkValues.getValueByIndex(i);
            if (currentValue - sla > threshold * sla) {
                consecutiveViolations++;
                violationValues.add(currentValue);
                if (consecutiveViolations == cwrong) {
                    return new Violation(i, sla, currentTime, violationValues);
                }
            } else {
                consecutiveViolations = 0;
                violationValues.clear();
            }

            if (consecutiveSamples == cwrong) {
                consecutiveSamples = 0;
                consecutiveViolations = 0;
                violationValues.clear();
                if (index + total < results.length) {
                    cwrong = results[index + total].getCwrong();
                } else {
                    cwrong = results[results.length - 1].getCwrong();
                }
            }
        }

        // If no violation found, return the last index (right trimming)
        int lastIndex = benchmarkValues.length() - 1;
        return new Violation(lastIndex, sla, benchmarkValues.getTimestampByIndex(lastIndex), null);
    }

    private static class Violation {
        int index;
        int sla;
        long timestamp;
        int[] values;

        Violation(int index, int sla, long timestamp, List<Integer> vals) {
            this.index = index;
            this.sla = sla;
            this.timestamp = timestamp;
            if (vals != null) {
                this.values = new int[vals.size()];
                for (int i = 0; i < vals.size(); i++) {
                    this.values[i] = vals.get(i);
                }
            }
        }

        String getMaxValueString() {
            if (values == null) {
                return "";
            }

            int max = -1;
            for (int value : values) {
                if (value > max) {
                    max = value;
                }
            }
            return "d=" + max;
        }

        String getValueString() {
            StringBuilder builder = new StringBuilder("");
            if (values != null) {
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) {
                        builder.append(" ");
                    }
                    builder.append("d=").append(values[i] - sla);
                }
            }
            return builder.toString();
        }
    }
}
