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

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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
     * Whether the predictions should be made by aggregating
     * multiple time series data into a single time series.
     */
    private boolean aggregateTimeSeries;

    /**
     * Path to an existing Cerebro trace file, from which the
     * program execution paths can be extracted.
     */
    private String traceFile;

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

    public void setAggregateTimeSeries(boolean aggregateTimeSeries) {
        this.aggregateTimeSeries = aggregateTimeSeries;
    }

    public String getTraceFile() {
        return traceFile;
    }

    public void setTraceFile(String traceFile) {
        this.traceFile = traceFile;
    }

    public void validate() throws Exception {
        if (benchmarkDataDir == null && benchmarkDataSvc == null) {
            throw new Exception("One of benchmark data directory and benchmark data service" +
                    " must be specified.");
        } else if (benchmarkDataDir != null && benchmarkDataSvc != null) {
            throw new Exception("Both benchmark data directory and benchmark data service" +
                    " should not be specified.");
        } else if (traceFile == null) {
            throw new Exception("Trace file path must be specified.");
        }
    }
}
