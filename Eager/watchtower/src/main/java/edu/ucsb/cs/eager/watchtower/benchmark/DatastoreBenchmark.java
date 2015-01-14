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

package edu.ucsb.cs.eager.watchtower.benchmark;

import com.google.appengine.api.datastore.*;
import edu.ucsb.cs.eager.watchtower.Constants;

import javax.servlet.ServletException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DatastoreBenchmark extends APIBenchmark {

    private static final Key root = KeyFactory.createKey("StudentRoot", "StudentRoot");

    private static final String STUDENT_KIND = "Student";
    public static final String PROJECT_ID = "project_id";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String LICENSE = "license";

    public static final String PROJECT_KIND = "Project";

    @Override
    public String getName() {
        return "Datastore";
    }

    @Override
    public Map<String, Integer> benchmark() throws ServletException {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        String projectId = UUID.randomUUID().toString();
        Map<String,Integer> results = new HashMap<String, Integer>();

        // put
        results.put(Constants.Datastore.PUT, putEntity(datastore, projectId));
        sleep(100);

        // get
        results.put(Constants.Datastore.GET, getEntity(datastore, projectId));
        sleep(100);

        // delete
        results.put(Constants.Datastore.DELETE, deleteEntity(datastore, projectId));
        sleep(100);

        // asList (lim = 10)
        results.put(Constants.Datastore.AS_LIST_10, asListWithLimit(datastore, 10));
        sleep(100);

        // asList (lim = 100)
        results.put(Constants.Datastore.AS_LIST_100, asListWithLimit(datastore, 100));
        sleep(100);

        // asList (lim = 100)
        results.put(Constants.Datastore.AS_LIST_1000, asListWithLimit(datastore, 1000));

        return results;
    }

    private int putEntity(DatastoreService datastore, String id) {
        Entity project = new Entity(PROJECT_KIND, id);
        project.setProperty(PROJECT_ID, id);
        project.setProperty(NAME, "Project-" + id);
        project.setProperty(DESCRIPTION, "Description");
        project.setProperty(LICENSE, "License");
        long start = System.currentTimeMillis();
        datastore.put(project);
        return (int)(System.currentTimeMillis() - start);
    }

    private int getEntity(DatastoreService datastore, String id) throws ServletException {
        Key key = KeyFactory.createKey(PROJECT_KIND, id);
        try {
            long start = System.currentTimeMillis();
            datastore.get(key);
            return (int) (System.currentTimeMillis() - start);
        } catch (EntityNotFoundException e) {
            throw new ServletException("Failed to locate object by key", e);
        }
    }

    private int deleteEntity(DatastoreService datastore, String id) {
        Key key = KeyFactory.createKey(PROJECT_KIND, id);
        long start = System.currentTimeMillis();
        datastore.delete(key);
        return (int) (System.currentTimeMillis() - start);
    }

    private int asListWithLimit(DatastoreService datastore, int limit) {
        Query q = new Query(STUDENT_KIND);
        PreparedQuery pq = datastore.prepare(q);
        long start = System.currentTimeMillis();
        List<Entity> list = pq.asList(FetchOptions.Builder.withLimit(limit));
        for (Entity student : list) {
        }
        return (int) (System.currentTimeMillis() - start);
    }

    public void init() {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        try {
            datastore.get(root);
        } catch (EntityNotFoundException e) {
            datastore.put(new Entity(root));
            for (int i = 0; i < 1000; i++) {
                Entity student = new Entity(STUDENT_KIND, root);
                student.setProperty("FirstName", "First" + i);
                student.setProperty("LastName", "Last" + i);
                datastore.put(student);
            }
        }
    }
}
