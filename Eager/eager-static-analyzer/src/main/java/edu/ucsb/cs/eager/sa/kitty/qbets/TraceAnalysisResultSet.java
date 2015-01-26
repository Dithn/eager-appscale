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

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates a collection of prediction results generated for a single method.
 * Each result represents a single path through the CFG of the method.
 */
public class TraceAnalysisResultSet {

    private List<TraceAnalysisResult[]> list = new ArrayList<>();

    public void addResult(TraceAnalysisResult[] result) {
        list.add(result);
    }

    public int size() {
        return list.size();
    }

    public TraceAnalysisResult[] get(int index) {
        return list.get(index);
    }

    public TraceAnalysisResult[] findLargest() {
        double max = 0.0;
        TraceAnalysisResult[] largest = null;
        for (int i = 0; i < list.size(); i++) {
            TraceAnalysisResult[] curr = list.get(i);
            int sum = 0;
            for (TraceAnalysisResult r : curr) {
                sum += r.approach2;
            }
            double average = ((double) sum) / curr.length;
            if (average > max) {
                max = average;
                largest = curr;
            }
        }
        return largest;
    }
}
