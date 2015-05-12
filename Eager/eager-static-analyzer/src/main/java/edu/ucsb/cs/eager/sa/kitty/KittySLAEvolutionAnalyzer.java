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

import edu.ucsb.cs.eager.sa.kitty.qbets.PathResult;
import edu.ucsb.cs.eager.sa.kitty.qbets.TimeSeries;
import edu.ucsb.cs.eager.sa.kitty.qbets.TraceAnalysisResult;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.util.*;

/**
 * KittySLAEvolutionAnalyzer performs an analysis similar to KittyValidator, but
 * focuses on the 3c violation category (see KittyValidator API docs for more
 * information on violation category). In addition to the time-to-violation, it
 * also provides insight to how the exact SLA predictions change on violation
 * points, and when the SLA violations occur, by exactly how much the actual API
 * benchmark value exceeds the predicted SLA.
 */
public class KittySLAEvolutionAnalyzer {

    public static void main(String[] args) throws IOException {
        SLAEvolutionAnalyzerConfig configMaker = new SLAEvolutionAnalyzerConfig();
        Config config;
        try {
            config = configMaker.construct(args, "KittySLAEvolutionAnalyzer");
        } catch (ConfigException e) {
            System.err.println(e.getMessage());
            return;
        }

        String benchmarkFile = configMaker.getOptionValue("bf");
        if (benchmarkFile == null) {
            System.err.println("benchmark file must be specified.");
            return;
        }
        boolean adaptiveIntervals = configMaker.hasOption("ai");
        boolean maxViolationOnly = configMaker.hasOption("mv");
        boolean eventCounting = configMaker.hasOption("ec");

        double threshold = 0.0;
        String thresholdString = configMaker.getOptionValue("vt");
        if (thresholdString != null) {
            threshold = Double.parseDouble(thresholdString);
        }

        double slaDiffThreshold = 0.0;
        String slaDiffThresholdString = configMaker.getOptionValue("sd");
        if (slaDiffThresholdString != null) {
            slaDiffThreshold = Double.parseDouble(slaDiffThresholdString);
        }

        KittySLAEvolutionAnalyzer analyzer = new KittySLAEvolutionAnalyzer();
        analyzer.adaptiveIntervals = adaptiveIntervals;
        analyzer.maxViolationOnly = maxViolationOnly;
        analyzer.eventCounting = eventCounting;
        analyzer.threshold = threshold;
        analyzer.slaDiffThreshold = slaDiffThreshold;
        analyzer.run(config, benchmarkFile);
    }

    private boolean adaptiveIntervals;
    private boolean eventCounting;
    private boolean maxViolationOnly;
    private double threshold;
    private double slaDiffThreshold;

    public void run(Config config, String benchmarkFile) throws IOException {
        TimeSeries benchmarkValues = PredictionUtils.parseBenchmarkFile(benchmarkFile);
        // Pull data from 1 day back at most. Otherwise the analysis is going to take forever.
        long start = benchmarkValues.getTimestampByIndex(0) - 3600 * 24 * 1000;
        long end = benchmarkValues.getTimestampByIndex(benchmarkValues.length() - 1);

        PathResult pathResult = Kitty.makePredictions(config, start, end);
        if (pathResult == null) {
            System.err.println("Failed to find the method: " + config.getMethods()[0]);
            return;
        }
        System.out.println();

        TraceAnalysisResult[] result = pathResult.getResults();
        int startIndex = PredictionUtils.findClosestIndex(
                benchmarkValues.getTimestampByIndex(0), result);
        if (startIndex < 0) {
            System.err.println("Prediction time-line do not overlap with benchmark time-line.");
            return;
        }

        if (adaptiveIntervals) {
            if (eventCounting) {
                adaptiveIntervalEventAnalysis(benchmarkValues, result, startIndex);
            } else {
                adaptiveIntervalAnalysis(benchmarkValues, result, startIndex, slaDiffThreshold);
            }
        } else {
            fixedIntervalAnalysis(benchmarkValues, result, startIndex);
        }
    }

    private void adaptiveIntervalEventAnalysis(TimeSeries benchmarkValues,
                                               TraceAnalysisResult[] result, int startIndex) {
        Map<Long,List<ViolationEvent>> events = new TreeMap<>();
        List<ViolationEvent> endOfTrace = new ArrayList<>();
        for (int i = 0; i < result.length - 1 - startIndex; i++) {
            int currentIndex = startIndex + i;
            while (currentIndex > 0 && currentIndex < result.length - 1) {
                Violation violation = findViolation(benchmarkValues, result, currentIndex,
                        result[currentIndex].getApproach2(), threshold);
                int nextIndex = PredictionUtils.findNextIndex(violation.timestamp, result);
                int nextSla = nextIndex > 0 ? result[nextIndex].getApproach2() : -1;
                ViolationEvent event = new ViolationEvent(violation, nextSla);
                if (nextIndex > 0) {
                    List<ViolationEvent> list = events.get(event.timestamp);
                    if (list == null) {
                        list = new ArrayList<>();
                        events.put(event.timestamp, list);
                    }
                    list.add(event);
                } else {
                    endOfTrace.add(event);
                }
                currentIndex = nextIndex;
            }
        }

        System.out.println("[violation] timestamp deltaMean deltaStdDev events");
        for (Map.Entry<Long,List<ViolationEvent>> entry : events.entrySet()) {
            List<Integer> deltas = new ArrayList<>();
            for (ViolationEvent event : entry.getValue()) {
                deltas.add(event.newSla - event.oldSla);
            }
            double mean = PredictionUtils.mean(deltas);
            double stdDev = PredictionUtils.stdDev(deltas, mean);
            System.out.printf("[violation] %d %7.2f %7.2f %5d\n", entry.getKey(), mean,
                    stdDev, deltas.size());
        }

        int lastIndex = benchmarkValues.length() - 1;
        System.out.printf("[eot] %d --- --- %5d\n", benchmarkValues.getTimestampByIndex(
                lastIndex), endOfTrace.size());

        System.out.println("\n");
        for (Map.Entry<Long,List<ViolationEvent>> entry : events.entrySet()) {
            System.out.printf("\n---- Change Point %d ----\n", entry.getKey());
            Map<Integer,Integer> diffCounts = new TreeMap<>();
            for (ViolationEvent event : entry.getValue()) {
                int diff = event.newSla - event.oldSla;
                System.out.printf("[cp][%d] %d --> %d (diff: %d)\n", entry.getKey(), event.oldSla,
                        event.newSla, diff);
                if (diffCounts.containsKey(diff)) {
                    diffCounts.put(diff, diffCounts.get(diff) + 1);
                } else {
                    diffCounts.put(diff, 1);
                }
            }

            System.out.println();
            for (Map.Entry<Integer,Integer> de : diffCounts.entrySet()) {
                System.out.printf("[cpsummary][%d] %d %d\n", entry.getKey(),
                        de.getKey(), de.getValue());
            }
        }
    }

    private void adaptiveIntervalAnalysis(TimeSeries benchmarkValues, TraceAnalysisResult[] result,
                                          int startIndex, double slaDiffThreshold) {
        System.out.println("[sla][offset] timestamp prediction timeToViolation(h) violationDelta");
        for (int i = 0; i < result.length - 1 - startIndex; i++) {
            int currentIndex = startIndex + i;
            int sla = result[currentIndex].getApproach2();
            long ts = result[currentIndex].getTimestamp();

            while (currentIndex >= 0 && currentIndex < result.length - 1) {
                Violation violation = findViolation(benchmarkValues, result, currentIndex,
                        sla, threshold);
                int nextIndex = PredictionUtils.findNextIndex(violation.timestamp, result);
                double slaDiff;
                if (nextIndex >= 0) {
                    slaDiff = Math.abs((result[nextIndex].getApproach2() - sla) / (double) sla);
                } else {
                    // End of trace -- must print this event
                    slaDiff = 1.0;
                }

                if (slaDiff >= slaDiffThreshold) {
                    String violationDiff = maxViolationOnly ? violation.getMaxValueString() :
                            violation.getValueString();
                    System.out.printf("[sla] %d %d %5d %-8s %s\n", i, ts, sla,
                            PredictionUtils.getTimeInHours(ts, violation.timestamp),
                            violationDiff);
                    if (nextIndex >= 0) {
                        ts = result[nextIndex].getTimestamp();
                        sla = result[nextIndex].getApproach2();
                    }
                }
                currentIndex = nextIndex;
            }
        }
    }

    private void fixedIntervalAnalysis(TimeSeries benchmarkValues, TraceAnalysisResult[] result,
                                          int startIndex) {

        long interval = getInterval(benchmarkValues, result, startIndex, threshold);
        System.out.println("Calculated interval: " + interval/1000.0/3600.0 + " hours\n");

        int currentIndex = startIndex;
        System.out.println("[sla] timestamp prediction timeToViolation(h)");
        while (currentIndex >= 0 && currentIndex < result.length - 1) {
            TraceAnalysisResult currentPrediction = result[currentIndex];
            Violation violation = findViolation(benchmarkValues, result, currentIndex,
                    currentPrediction.getApproach2(), threshold);
            System.out.printf("[sla] %d %5d %-8s\n", currentPrediction.getTimestamp(),
                    currentPrediction.getApproach2(),
                    PredictionUtils.getTimeInHours(currentPrediction.getTimestamp(),
                            violation.timestamp));
            currentIndex = PredictionUtils.findNextIndex(
                    currentPrediction.getTimestamp() + interval, result);
        }
    }

    private long getInterval(TimeSeries benchmarkValues, TraceAnalysisResult[] result,
                             int startIndex, double threshold) {
        List<Long> violationTimes = new ArrayList<>();
        for (int i = startIndex; i < result.length; i+=15) {
            Violation violation = findViolation(benchmarkValues, result, i,
                    result[i].getApproach2(), threshold);
            violationTimes.add(violation.timestamp - result[i].getTimestamp());
        }
        Collections.sort(violationTimes);
        int fifthPercentileIndex = (int) Math.ceil(0.05 * violationTimes.size());
        return violationTimes.get(fifthPercentileIndex);
    }

    private Violation findViolation(TimeSeries benchmarkValues, TraceAnalysisResult[] results,
                                    int index, int sla, double threshold) {
        int consecutiveViolations = 0, consecutiveSamples = 0, total = 0;
        TraceAnalysisResult r = results[index];
        int cwrong = r.getCwrong();
        List<Integer> violationValues = new ArrayList<>();

        for (int i = 0; i < benchmarkValues.length(); i++) {
            long currentTime = benchmarkValues.getTimestampByIndex(i);
            if (currentTime < r.getTimestamp()) {
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

    private static class SLAEvolutionAnalyzerConfig extends ConfigMaker {
        @Override
        protected void preConstruction(Options options) {
            addOption(options, "bf", "benchmark-file", true, "File containing benchmark data");
            addOption(options, "ai", "adaptive-intervals", false, "Enable adaptive interval analysis");
            addOption(options, "mv", "max-violation", false, "Output only the max SLA violations");
            addOption(options, "ec", "event-counter", false, "Enable event counting mode");
            addOption(options, "vt", "violation-threshold", true, "Threshold value used to detect SLA violations");
            addOption(options, "sd", "sla-diff-threshold", true, "Threshold diff value between two SLAs to detect a change point");
        }

        @Override
        protected void postConstruction(Config config) throws ConfigException {
            if (config.getMethods() == null || config.getMethods().length != 1) {
                throw new ConfigException("one method must be specified for analysis.");
            }
        }
    }

    private static class ViolationEvent {
        int oldSla, newSla;
        long timestamp;
        int[] values;

        ViolationEvent(Violation v, int newSla) {
            oldSla = v.sla;
            timestamp = v.timestamp;
            values = v.values;
            this.newSla = newSla;
        }
    }

    private static class Violation {
        int index;
        int sla;
        long timestamp;
        int[] values;

        Violation(int index, int sla, long timestamp, List<Integer> values) {
            this.index = index;
            this.sla = sla;
            this.timestamp = timestamp;
            if (values != null) {
                this.values = new int[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    this.values[i] = values.get(i);
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
