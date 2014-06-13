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

package edu.ucsb.cs.eager.cb.appscale.datastore;

import com.google.appengine.api.datastore.*;
import edu.ucsb.cs.eager.cb.appscale.JSONUtils;
import edu.ucsb.cs.eager.cb.appscale.ServletUtils;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class DatastoreBenchmarkingServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {
        String op = ServletUtils.getRequiredParameter(req, "op");
        int iterations = ServletUtils.getOptionalIntParameter(req, "count", 1);
        if (iterations <= 0) {
            throw new ServletException("Number of iterations must be positive");
        }

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        List<Integer> results = new ArrayList<Integer>();
        if ("put".equals(op)) {
            for (int i = 0; i < iterations; i++) {
                String projectId = UUID.randomUUID().toString();
                String projectName = "Project" + i;
                Entity project = new Entity(Constants.Project.class.getSimpleName(), projectName);
                project.setProperty(Constants.Project.PROJECT_ID, projectId);
                project.setProperty(Constants.Project.NAME, projectName);
                project.setProperty(Constants.Project.DESCRIPTION, "Description" + i);
                project.setProperty(Constants.Project.LICENSE, "License" + i);
                project.setProperty(Constants.Project.RATING, 5);
                project.setProperty(Constants.TYPE, Constants.Project.TYPE_VALUE);
                long t1 = System.currentTimeMillis();
                datastore.put(project);
                long t2 = System.currentTimeMillis();
                results.add((int) (t2 - t1));
            }
        } else if ("asIterable".equals(op)) {
            for (int i = 0; i < iterations; i++) {
                Query q = new Query(Constants.Project.class.getSimpleName());
                PreparedQuery preparedQuery = datastore.prepare(q);
                long t1 = System.currentTimeMillis();
                preparedQuery.asIterable(); // Todo: How to handle this streaming op?
                long t2 = System.currentTimeMillis();
                results.add((int) (t2 - t1));
            }
        } else if ("asList".equals(op)) {
            for (int i = 0; i < iterations; i++) {
                Query q = new Query(Constants.Project.class.getSimpleName());
                PreparedQuery preparedQuery = datastore.prepare(q);
                FetchOptions fetchOptions = FetchOptions.Builder.withDefaults();
                long t1 = System.currentTimeMillis();
                preparedQuery.asList(fetchOptions);
                long t2 = System.currentTimeMillis();
                results.add((int) (t2 - t1));
            }
        } else if ("get".equals(op)) {
            for (int i = 0; i < iterations; i++) {
                String projectName = "Project" + i;
                Key key = KeyFactory.createKey(Constants.Project.class.getSimpleName(), projectName);
                long t1 = System.currentTimeMillis();
                try {
                    datastore.get(key);
                } catch (EntityNotFoundException e) {
                    throw new ServletException("Failed to locate object by key", e);
                }
                long t2 = System.currentTimeMillis();
                results.add((int) (t2 - t1));
            }
        } else if ("delete".equals(op)) {
            for (int i = 0; i < iterations; i++) {
                String projectName = "Project" + i;
                Key key = KeyFactory.createKey(Constants.Project.class.getSimpleName(), projectName);
                long t1 = System.currentTimeMillis();
                datastore.delete(key);
                long t2 = System.currentTimeMillis();
                results.add((int) (t2 - t1));
            }
        } else if ("jdo.makePersistent".equals(op)) {
            for (int i = 0; i < iterations; i++) {
                String projectId = "Project" + i;
                Key key = KeyFactory.createKey(Project.class.getSimpleName(), projectId);
                Project project = new Project(key, "Project" + i, 5);
                PersistenceManager pm = PMF.get().getPersistenceManager();
                try {
                    long t1 = System.currentTimeMillis();
                    pm.makePersistent(project);
                    long t2 = System.currentTimeMillis();
                    results.add((int) (t2 - t1));
                } finally {
                    pm.close();
                }
            }
        } else if ("jdo.getObjectById".equals(op)) {
            for (int i = 0; i < iterations; i++) {
                String projectId = "Project" + i;
                Key key = KeyFactory.createKey(Project.class.getSimpleName(), projectId);
                PersistenceManager pm = PMF.get().getPersistenceManager();
                try {
                    long t1 = System.currentTimeMillis();
                    pm.getObjectById(Project.class, key);
                    long t2 = System.currentTimeMillis();
                    results.add((int) (t2 - t1));
                } finally {
                    pm.close();
                }
            }
        } else if ("jdo.close".equals(op)) {
            for (int i = 0; i < iterations; i++) {
                String projectId = "Project" + i;
                Key key = KeyFactory.createKey(Project.class.getSimpleName(), projectId);
                PersistenceManager pm = PMF.get().getPersistenceManager();
                try {
                    pm.getObjectById(Project.class, key);
                } finally {
                    long t1 = System.currentTimeMillis();
                    pm.close();
                    long t2 = System.currentTimeMillis();
                    results.add((int) (t2 - t1));
                }
            }
        } else if ("jdo.execute".equals(op)) {
                for (int i = 0; i < iterations; i++) {
                    String projectId = "Project" + i;
                    PersistenceManager pm = PMF.get().getPersistenceManager();
                    javax.jdo.Query query = pm.newQuery(Project.class);
                    try {
                        query.setFilter("name == " + projectId);
                        long t1 = System.currentTimeMillis();
                        query.execute();
                        long t2 = System.currentTimeMillis();
                        results.add((int) (t2 - t1));
                    } finally {
                        query.closeAll();
                        pm.close();
                    }
                }
        } else if ("jdo.closeAll".equals(op)) {
            for (int i = 0; i < iterations; i++) {
                String projectId = "Project" + i;
                PersistenceManager pm = PMF.get().getPersistenceManager();
                javax.jdo.Query query = pm.newQuery(Project.class);
                try {
                    query.setFilter("name == " + projectId);
                    query.execute();
                } finally {
                    long t1 = System.currentTimeMillis();
                    query.closeAll();
                    long t2 = System.currentTimeMillis();
                    results.add((int) (t2 - t1));
                    pm.close();
                }
            }
        } else if ("jdo.deletePersistent".equals(op)) {
            for (int i = 0; i < iterations; i++) {
                String projectId = "Project" + i;
                Key key = KeyFactory.createKey(Project.class.getSimpleName(), projectId);
                PersistenceManager pm = PMF.get().getPersistenceManager();
                try {
                    Project project = pm.getObjectById(Project.class, key);
                    long t1 = System.currentTimeMillis();
                    pm.deletePersistent(project);
                    long t2 = System.currentTimeMillis();
                    results.add((int) (t2 - t1));
                } finally {
                    pm.close();
                }
            }
        } else {
            throw new ServletException("Unsupported datastore benchmark operation: " + op);
        }
        sendOutput(results, op, resp);
    }

    private void sendOutput(List<Integer> results, String op,
                            HttpServletResponse resp) throws IOException {
        Map<String,Object> output = new HashMap<String, Object>();
        output.put("api", "datastore");
        output.put("operation", op);
        output.put("iterations", results.size());
        int sum = 0;
        for (int value : results) {
            sum += value;
        }
        double mean = ((double) sum) / results.size();
        output.put("average", mean);

        if (results.size() > 1) {
            double squareSum = 0;
            for (int value : results) {
                squareSum += (value - mean) * (value - mean);
            }
            double stdDev = Math.sqrt(squareSum / (results.size() - 1));
            output.put("stdDev", stdDev);
        } else {
            output.put("stdDev", 0);
        }
        JSONUtils.serialize(output, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req,
                            HttpServletResponse resp) throws ServletException, IOException {
        cleanup();
    }

    private void cleanup() {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query q = new Query(Constants.Project.class.getSimpleName());
        PreparedQuery preparedQuery = datastore.prepare(q);
        for (Entity result : preparedQuery.asIterable()) {
            datastore.delete(result.getKey());
        }
    }
}
