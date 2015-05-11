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

import edu.ucsb.cs.eager.sa.kitty.ConfigException;

public class QBETSConfig {

    private long start, end;
    private double quantile = 0.95, confidence = 0.05;
    private String benchmarkDataSvc;
    private boolean disableApproach1;

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

    public String getBenchmarkDataSvc() {
        return benchmarkDataSvc;
    }

    public void setBenchmarkDataSvc(String benchmarkDataSvc) {
        this.benchmarkDataSvc = benchmarkDataSvc;
    }

    public boolean isDisableApproach1() {
        return disableApproach1;
    }

    public void setDisableApproach1(boolean disableApproach1) {
        this.disableApproach1 = disableApproach1;
    }

    public void validate() throws ConfigException {
        if (quantile < 0 || quantile > 1) {
            throw new ConfigException("Quantile must be in the interval [0,1]");
        } else if (confidence < 0 || confidence > 1) {
            throw new ConfigException("Confidence must be in the interval [0,1]");
        } else if (end < start) {
            throw new ConfigException("End timestamp must be greater than or equal" +
                    " to start timestamp.");
        }
    }
}
