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

import com.google.appengine.api.datastore.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataPoint {

    private long timestamp;
    private Map<String,Integer> data = new HashMap<String, Integer>();

    private static final Object lock = new Object();

    public DataPoint(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void put(String key, int value) {
        this.data.put(key, value);
    }

    public void putAll(Map<String,Integer> map) {
        this.data.putAll(map);
    }

    public Map<String,Integer> getData() {
        return this.data;
    }

    public void save() {
        synchronized (DataPoint.lock) {
            DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
            datastore.put(toEntity());
        }
    }

    private Entity toEntity() {
        Entity entity = new Entity(Constants.DATA_POINT_KIND, Constants.DATA_POINT_PARENT);
        entity.setProperty(Constants.DATA_POINT_TIMESTAMP, timestamp);
        for (Map.Entry<String,Integer> entry : data.entrySet()) {
            entity.setProperty(entry.getKey(), entry.getValue());
        }
        return entity;
    }

    public static List<DataPoint> getAll() {
        Query q = new Query(Constants.DATA_POINT_KIND).
                addSort(Constants.DATA_POINT_TIMESTAMP, Query.SortDirection.ASCENDING);

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        PreparedQuery pq = datastore.prepare(q);
        List<DataPoint> data = new ArrayList<DataPoint>();
        for (Entity entity : pq.asIterable()) {
            DataPoint p = new DataPoint((Long) entity.getProperty(Constants.DATA_POINT_TIMESTAMP));
            Map<String,Object> props = entity.getProperties();
            for (Map.Entry<String,Object> entry : props.entrySet()) {
                if (entry.getKey().startsWith("bm_")) {
                    // AppEngine turns integers into longs.
                    // So the value returned here would be a Long.
                    long value = (Long) entry.getValue();
                    p.put(entry.getKey(), (int) value);
                }
            }
            data.add(p);
        }
        return data;
    }

    public static void restore(List<DataPoint> dataPoints) {
        synchronized (DataPoint.lock) {
            Query q = new Query(Constants.DATA_POINT_KIND).
                    addSort(Constants.DATA_POINT_TIMESTAMP, Query.SortDirection.ASCENDING);
            DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
            PreparedQuery pq = datastore.prepare(q);
            for (Entity entity : pq.asIterable()) {
                datastore.delete(entity.getKey());
            }

            List<Entity> entities = new ArrayList<Entity>();
            for (DataPoint p : dataPoints) {
                entities.add(p.toEntity());
            }
            datastore.put(entities);
        }
    }
}
