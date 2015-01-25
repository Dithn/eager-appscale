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

package edu.ucsb.cs.eager.watchtower;

import java.util.*;

public class QueryResult {

    private List<Long> timestamps = new ArrayList<Long>();
    private Map<String,List<Integer>> benchmarkData = new HashMap<String, List<Integer>>();

    public void add(long ts, Map<String,Integer> data) {
        int count = timestamps.size();
        if (count > 0 && timestamps.get(count - 1) > ts) {
            throw new IllegalStateException("Timestamp order violation");
        }

        timestamps.add(ts);
        for (Map.Entry<String,Integer> entry : data.entrySet()) {
            if (!benchmarkData.containsKey(entry.getKey())) {
                benchmarkData.put(entry.getKey(), new ArrayList<Integer>());
            }
            benchmarkData.get(entry.getKey()).add(entry.getValue());
        }
    }

    public List<Long> getTimestamps() {
        return Collections.unmodifiableList(timestamps);
    }

    public Map<String, List<Integer>> getBenchmarkData() {
        return Collections.unmodifiableMap(benchmarkData);
    }
}
