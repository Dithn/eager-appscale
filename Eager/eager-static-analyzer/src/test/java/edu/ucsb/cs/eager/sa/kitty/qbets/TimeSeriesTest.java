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

import junit.framework.TestCase;
import org.json.JSONArray;

public class TimeSeriesTest extends TestCase {

    public void testTimeSeriesOrder() {
        TimeSeries ts = new TimeSeries();
        for (int i = 0; i < 1000; i++) {
            ts.add(i * i, i);
        }
        assertEquals(1000, ts.length());

        for (int i = 0; i < 1000; i++) {
            assertEquals(ts.getTimestampByIndex(i), i * i);
            assertEquals(ts.getByIndex(i), i);
        }

        try {
            ts.add(100, 100);
            fail("no exception thrown when adding entry with smaller timestamp");
        } catch (Exception ignored) {
        }
    }

    public void testTimeSeriesAggregation() {
        TimeSeries ts1 = new TimeSeries();
        for (int i = 0; i < 1000; i++) {
            ts1.add(i * i, i);
        }

        TimeSeries ts2 = new TimeSeries();
        for (int i = 0; i < 1000; i++) {
            ts2.add(i * i, 2 * i);
        }

        TimeSeries aggr = ts1.aggregate(ts2);
        assertEquals(1000, aggr.length());
        for (int i = 0; i < 1000; i++) {
            assertEquals(aggr.getTimestampByIndex(i), i * i);
            assertEquals(aggr.getByIndex(i), 3 * i);
        }
    }

    public void testTimeSeriesJSON() {
        TimeSeries ts = new TimeSeries();
        for (int i = 0; i < 1000; i++) {
            ts.add(i * i, i);
        }
        JSONArray array = ts.toJSON();
        assertEquals(1000, array.length());

        TimeSeries copy = new TimeSeries(array, 1000);
        assertEquals(1000, copy.length());
        for (int i = 0; i < 1000; i++) {
            assertEquals(copy.getTimestampByIndex(i), i * i);
            assertEquals(copy.getByIndex(i), i);
        }
    }

}
