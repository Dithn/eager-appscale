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

import edu.ucsb.cs.eager.sa.kitty.Identifiable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TimeSeriesDataCache {

    private Map<String,TimeSeries> ts;
    private Map<String,TimeSeries> quantiles = new ConcurrentHashMap<>();

    public TimeSeriesDataCache(Map<String,TimeSeries> ts) {
        this.ts = ts;
    }

    public TimeSeries getTimeSeries(Identifiable op) {
        return ts.get(op.getId());
    }

    public void putQuantiles(Identifiable op, int pathLength, TimeSeries value) {
        quantiles.put(key(op, pathLength), value);
    }

    public TimeSeries getQuantiles(Identifiable op, int pathLength) {
        return quantiles.get(key(op, pathLength));
    }

    public boolean containsQuantiles(Identifiable op, int pathLength) {
        return quantiles.containsKey(key(op, pathLength));
    }

    private String key(Identifiable op, int pathLength) {
        return op.getId() + "_" + pathLength;
    }

    public int getTimeSeriesLength() {
        int length = Integer.MAX_VALUE;
        for (TimeSeries data : ts.values()) {
            if (data.length() < length) {
                length = data.length();
            }
        }
        return length;
    }
}
