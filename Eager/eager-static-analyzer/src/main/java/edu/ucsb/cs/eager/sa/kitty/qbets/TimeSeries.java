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
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TimeSeries represents a time series of data. Each entry consists of
 * a timestamp and an integer (a measurement). Timestamps are monotonically
 * increasing.
 */
public class TimeSeries {

    private static final String TIMESTAMP = "Timestamp";
    private static final String VALUE = "Value";
    private static final String CWRONG = "Cwrong";

    private List<Long> timestamps = new ArrayList<>();
    private List<Integer> values = new ArrayList<>();
    private List<Integer> cwrong = new ArrayList<>();

    /**
     * Create an empty TimeSeries
     */
    public TimeSeries(){
    }

    /**
     * Extract TimeSeries from a JSON payload. Only the last len elements in the JSON
     * array would be included in the resulting TimeSeries.
     *
     * @param array A JSON array representing a time series
     * @param len Last number of elements to be included in the result
     */
    public TimeSeries(JSONArray array, int len) {
        for (int i = 0; i < len; i++) {
            JSONObject dataPoint = array.getJSONObject(array.length() - len + i);
            int cw;
            try {
                cw = dataPoint.getInt(CWRONG);
            } catch (JSONException e) {
                cw = 0;
            }
            add(dataPoint.getLong(TIMESTAMP), dataPoint.getInt(VALUE), cw);
        }
    }

    public void add(long time, int val) {
        add(time, val, 0);
    }

    void add(long time, int val, int cw) {
        int len = timestamps.size();
        if (len > 0 && timestamps.get(len - 1) >= time) {
            throw new IllegalStateException("time values not increasing monotonically");
        }
        timestamps.add(time);
        values.add(val);
        cwrong.add(cw);
    }

    public int length() {
        return timestamps.size();
    }

    public int getValueByIndex(int index) {
        return values.get(index);
    }

    public int getCwrongByIndex(int index) {
        return cwrong.get(index);
    }

    public long getTimestampByIndex(int index) {
        return timestamps.get(index);
    }

    public int getByTimestamp(long ts) {
        int index = Collections.binarySearch(timestamps, ts);
        if (index >= 0) {
            return values.get(index);
        }
        throw new IllegalArgumentException("failed to find timestamp: " + ts);
    }

    public int getCwrongByTimestamp(long ts) {
        int index = Collections.binarySearch(timestamps, ts);
        if (index >= 0) {
            return cwrong.get(index);
        }
        throw new IllegalArgumentException("failed to find timestamp: " + ts);
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
            dp.put(TIMESTAMP, (long) timestamps.get(i));
            dp.put(VALUE, (int) values.get(i));
            dp.put(CWRONG, (int) cwrong.get(i));
            array.put(dp);
        }
        return array;
    }

}
