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

public class PredictionConfig {

    /**
     * Path to a directory containing API benchmark results.
     */
    private String benchmarkDataDir;
    /**
     * Number of simulations to run.
     */
    private int simulations = 100;

    /**
     * URL of the benchmark data service.
     */
    private String benchmarkDataSvc;
    /**
     * Execution time quantile that should be predicted.
     */
    private double quantile = 0.95;
    /**
     * Upper confidence of the predicted time quantile.
     */
    private double confidence = 0.05;
    /**
     * Whether the predictions should be made by aggregating
     * multiple time series data into a single time series.
     */
    private boolean aggregateTimeSeries;

    /**
     * Path to an existing Cerebro trace file, from which the
     * program execution paths can be extracted.
     */
    private String traceFile;

    /**
     * Start timestamp to use when fetching time series data.
     */
    private long start = -1L;

    /**
     * End timestamp to use when fetching time series data.
     */
    private long end = -1L;

    /**
     * Use the simple QBETS predictor, instead of the trace-based
     * predictor.
     */
    private boolean simplePredictor;

    private String cerebroClasspath;
    private String clazz;
    private boolean loadNecessaryClasses = true;
    private boolean wholeProgramMode;

    private String[] methods;

    private int maxEntities = 1000;

    public String getBenchmarkDataDir() {
        return benchmarkDataDir;
    }

    public void setBenchmarkDataDir(String benchmarkDataDir) {
        this.benchmarkDataDir = benchmarkDataDir;
    }

    public int getSimulations() {
        return simulations;
    }

    public void setSimulations(int simulations) {
        this.simulations = simulations;
    }

    public String getBenchmarkDataSvc() {
        return benchmarkDataSvc;
    }

    public void setBenchmarkDataSvc(String benchmarkDataSvc) {
        this.benchmarkDataSvc = benchmarkDataSvc;
    }

    public boolean isAggregateTimeSeries() {
        return aggregateTimeSeries;
    }

    public double getQuantile() {
        return quantile;
    }

    public void setQuantile(double quantile) {
        this.quantile = quantile;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public void setAggregateTimeSeries(boolean aggregateTimeSeries) {
        this.aggregateTimeSeries = aggregateTimeSeries;
    }

    public String getTraceFile() {
        return traceFile;
    }

    public void setTraceFile(String traceFile) {
        this.traceFile = traceFile;
    }

    public String getCerebroClasspath() {
        return cerebroClasspath;
    }

    public void setCerebroClasspath(String cerebroClasspath) {
        this.cerebroClasspath = cerebroClasspath;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public boolean isLoadNecessaryClasses() {
        return loadNecessaryClasses;
    }

    public void setLoadNecessaryClasses(boolean loadNecessaryClasses) {
        this.loadNecessaryClasses = loadNecessaryClasses;
    }

    public boolean isWholeProgramMode() {
        return wholeProgramMode;
    }

    public void setWholeProgramMode(boolean wholeProgramMode) {
        this.wholeProgramMode = wholeProgramMode;
    }

    public boolean isSimplePredictor() {
        return simplePredictor;
    }

    public void setSimplePredictor(boolean simplePredictor) {
        this.simplePredictor = simplePredictor;
    }

    public boolean isEnabledMethod(String method) {
        if (methods == null || methods.length == 0) {
            return true;
        }
        for (String m : methods) {
            if (m.equals(method)) {
                return true;
            }
        }
        return false;
    }

    public void setMethods(String[] methods) {
        this.methods = methods;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public int getMaxEntities() {
        return maxEntities;
    }

    public void setMaxEntities(int maxEntities) {
        this.maxEntities = maxEntities;
    }

    public void validate() throws Exception {
        if (benchmarkDataDir == null && benchmarkDataSvc == null) {
            throw new Exception("One of benchmark data directory and benchmark data service" +
                    " must be specified.");
        } else if (benchmarkDataDir != null && benchmarkDataSvc != null) {
            throw new Exception("Both benchmark data directory and benchmark data service" +
                    " should not be specified.");
        } else if (traceFile == null && cerebroClasspath == null) {
            throw new Exception("One of trace file path and Cerebro class path must be specified.");
        } else if (quantile < 0 || quantile > 1) {
            throw new Exception("Quantile must be in the interval [0,1]");
        } else if (confidence < 0 || confidence > 1) {
            throw new Exception("Confidence must be in the interval [0,1]");
        } else if (traceFile != null && cerebroClasspath != null) {
            throw new Exception("Trace file and Cerebro class path should not specified together.");
        } else if (cerebroClasspath != null && clazz == null) {
            throw new Exception("Class must be specified when Cerebro class path is provided.");
        } else if (end < start) {
            throw new Exception("End timestamp must be greater than or equal to start timestamp.");
        }
    }
}
