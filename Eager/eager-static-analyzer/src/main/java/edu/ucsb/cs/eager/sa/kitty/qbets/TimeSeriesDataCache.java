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

import java.util.HashMap;
import java.util.Map;

public class TimeSeriesDataCache {

    private Map<String,int[]> ts;
    private Map<String,Integer> quantiles = new HashMap<String, Integer>();

    public TimeSeriesDataCache(Map<String, int[]> ts) {
        this.ts = ts;
    }

    public int[] getTimeSeries(String op) {
        return ts.get(op);
    }

    public void putQuantile(String op, int pathLength, int tsPos, int value) {
        quantiles.put(key(op, pathLength, tsPos), value);
    }

    public int getQuantile(String op, int pathLength, int tsPos) {
        return quantiles.get(key(op, pathLength, tsPos));
    }

    public boolean containsQuantile(String op, int pathLength, int tsPos) {
        return quantiles.containsKey(key(op, pathLength, tsPos));
    }

    private String key(String op, int pathLength, int tsPos) {
        return op + "_" + pathLength + "_" + tsPos;
    }

    public int getTimeSeriesLength() {
        int length = Integer.MAX_VALUE;
        for (int[] data : ts.values()) {
            if (data.length < length) {
                length = data.length;
            }
        }
        return length;
    }
}
