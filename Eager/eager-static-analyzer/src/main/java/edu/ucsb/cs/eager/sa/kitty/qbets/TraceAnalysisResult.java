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

public class TraceAnalysisResult {

    // run QBETS on individual time series and sum the results
    int approach1;

    // aggregate the time series and run QBETS
    int approach2;

    // current data point in the aggregate time series
    int sum;

    public int getApproach1() {
        return approach1;
    }

    public int getApproach2() {
        return approach2;
    }

    public int getSum() {
        return sum;
    }
}
