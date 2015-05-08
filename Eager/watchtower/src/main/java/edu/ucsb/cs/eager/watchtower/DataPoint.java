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

import java.util.*;

public class DataPoint {

    private long timestamp;
    private Map<String,Integer> data = new HashMap<String, Integer>();

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
        return Collections.unmodifiableMap(this.data);
    }

    private Entity toEntity() {
        Entity entity = new Entity(Constants.DATA_POINT_KIND, Constants.DATA_POINT_PARENT);
        entity.setProperty(Constants.DATA_POINT_TIMESTAMP, timestamp);
        for (Map.Entry<String,Integer> entry : data.entrySet()) {
            entity.setProperty(entry.getKey(), entry.getValue());
        }
        return entity;
    }

    public boolean save() {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Transaction txn = datastore.beginTransaction();
        boolean result = true;
        try {
            datastore.put(txn, toEntity());
            txn.commit();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                result = false;
            }
        }
        return result;
    }

    public static List<DataPoint> getAll() {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query q = new Query(Constants.DATA_POINT_KIND, Constants.DATA_POINT_PARENT).
                addSort(Constants.DATA_POINT_TIMESTAMP, Query.SortDirection.ASCENDING);
        Transaction txn = datastore.beginTransaction();
        try {
            PreparedQuery pq = datastore.prepare(txn, q);
            Iterable<Entity> entities = pq.asIterable(FetchOptions.Builder.withChunkSize(100));
            List<DataPoint> data = toList(entities);
            txn.commit();
            return data;
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    public static List<DataPoint> getRange(long start, int limit) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query q = new Query(Constants.DATA_POINT_KIND, Constants.DATA_POINT_PARENT).
                addSort(Constants.DATA_POINT_TIMESTAMP, Query.SortDirection.ASCENDING);
        if (start != -1L) {
            q.setFilter(new Query.FilterPredicate(Constants.DATA_POINT_TIMESTAMP,
                    Query.FilterOperator.GREATER_THAN, start));
        }
        Transaction txn = datastore.beginTransaction();
        try {
            PreparedQuery pq = datastore.prepare(txn, q);
            Iterable<Entity> entities = pq.asIterable(FetchOptions.Builder.withChunkSize(100).
                    limit(limit));
            List<DataPoint> data = toList(entities);
            txn.commit();
            return data;
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    private static List<DataPoint> toList(Iterable<Entity> entities) {
        List<DataPoint> list = new ArrayList<DataPoint>();
        for (Entity entity : entities) {
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
            list.add(p);
        }
        return list;
    }

    public static boolean restore(List<DataPoint> dataPoints) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Transaction txn = datastore.beginTransaction();

        boolean result = true;
        try {
            Query q = new Query(Constants.DATA_POINT_KIND, Constants.DATA_POINT_PARENT).
                    addSort(Constants.DATA_POINT_TIMESTAMP, Query.SortDirection.ASCENDING);
            PreparedQuery pq = datastore.prepare(txn, q);
            List<Key> keys = new ArrayList<Key>();
            for (Entity entity : pq.asIterable()) {
                keys.add(entity.getKey());
            }
            datastore.delete(txn, keys);

            List<Entity> entities = new ArrayList<Entity>();
            for (DataPoint p : dataPoints) {
                entities.add(p.toEntity());
            }
            datastore.put(txn, entities);
            txn.commit();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                result = false;
            }
        }
        return result;
    }
}
