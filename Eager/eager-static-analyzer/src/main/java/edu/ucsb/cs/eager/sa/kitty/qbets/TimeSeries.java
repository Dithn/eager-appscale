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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * TimeSeries represents a time series of data. Each entry consists of
 * a timestamp and an integer (a measurement). Timestamps are monotonically
 * increasing.
 */
public class TimeSeries {

    private List<Long> timestamps = new ArrayList<Long>();
    private List<Integer> values = new ArrayList<Integer>();

    public void add(long time, int val) {
        int len = timestamps.size();
        if (len > 0 && timestamps.get(len - 1) >= time) {
            throw new IllegalStateException("time values not increasing monotonically");
        }
        timestamps.add(time);
        values.add(val);
    }

    public int length() {
        return timestamps.size();
    }

    public int getByIndex(int index) {
        return values.get(index);
    }

    public long getTimestampByIndex(int index) {
        return timestamps.get(index);
    }

    public TimeSeries aggregate(TimeSeries other) {
        TimeSeries aggregate = new TimeSeries();
        for (int i = 0; i < timestamps.size(); i++) {
            if (timestamps.get(i).equals(other.timestamps.get(i))) {
                aggregate.add(timestamps.get(i), values.get(i) + other.values.get(i));
            }
        }
        return aggregate;
    }

    public JSONArray toJSON() {
        JSONArray array = new JSONArray();
        for (int i = 0; i < timestamps.size(); i++) {
            JSONObject dp = new JSONObject();
            dp.put("Timestamp", (long) timestamps.get(i));
            dp.put("Value", (int) values.get(i));
            array.put(dp);
        }
        return array;
    }

}
