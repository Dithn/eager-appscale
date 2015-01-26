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

import edu.ucsb.cs.eager.sa.kitty.APICall;
import edu.ucsb.cs.eager.sa.kitty.Path;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class TimeSeriesDataCacheTest extends TestCase {

    public void testCache() {
        Map<String,TimeSeries> map = new HashMap<>();
        map.put("bm_datastore_get", getTimeSeries(1000));
        map.put("bm_datastore_put", getTimeSeries(1500));
        TimeSeriesDataCache cache = new TimeSeriesDataCache(map);

        TimeSeries ts = cache.getTimeSeries(new APICall("com.google.appengine.api.datastore.DatastoreService#get()"));
        assertEquals(1000, ts.length());

        ts = cache.getTimeSeries(new APICall("com.google.appengine.api.datastore.DatastoreService#put()"));
        assertEquals(1500, ts.length());

        ts = cache.getTimeSeries(new APICall("com.google.appengine.api.datastore.DatastoreService#delete()"));
        assertNull(ts);

        assertEquals(1000, cache.getTimeSeriesLength());

        TimeSeries q = getTimeSeries(750);
        APICall op = new APICall("com.google.appengine.api.datastore.DatastoreService#get()");
        cache.putQuantiles(op, 2, q);
        ts = cache.getQuantiles(op, 2);
        assertEquals(750, ts.length());

        assertNull(cache.getQuantiles(op, 3));
    }

    private TimeSeries getTimeSeries(int len) {
        TimeSeries ts = new TimeSeries();
        for (int i = 0; i < len; i++) {
            ts.add(i * i, i);
        }
        return ts;
    }

}
