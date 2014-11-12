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

package edu.ucsb.cs.eager.watchtower.benchmark.jdo;

import com.google.appengine.api.datastore.*;
import edu.ucsb.cs.eager.watchtower.Constants;
import edu.ucsb.cs.eager.watchtower.benchmark.APIBenchmark;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import java.util.HashMap;
import java.util.Map;

public class DatastoreJDOBenchmark extends APIBenchmark {

    @Override
    public String getName() {
        return "Datastore-JDO";
    }

    @Override
    public Map<String, Integer> benchmark() throws ServletException {
        String projectId = "TestProjectId";
        Map<String,Integer> results = new HashMap<String, Integer>();

        // makePersistent
        results.put(Constants.DatastoreJDO.MAKE_PERSISTENT, makePersistent(projectId));
        sleep(1000);

        // execute
        results.put(Constants.DatastoreJDO.EXECUTE, execute(projectId));
        sleep(1000);

        // delete
        results.put(Constants.DatastoreJDO.DELETE, delete(projectId));
        sleep(1000);

        return results;
    }

    private int makePersistent(String id) {
        Key key = KeyFactory.createKey(Project.class.getSimpleName(), id);
        Project project = new Project(key, id, "Description", "License");
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            long t1 = System.currentTimeMillis();
            pm.makePersistent(project);
            return (int) (System.currentTimeMillis() - t1);
        } finally {
            pm.close();
        }
    }

    private int execute(String id) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        javax.jdo.Query query = pm.newQuery(Project.class);
        try {
            query.setFilter("name == " + id);
            long t1 = System.currentTimeMillis();
            query.execute();
            return (int) (System.currentTimeMillis() - t1);
        } finally {
            query.closeAll();
            pm.close();
        }
    }

    private int delete(String id) {
        Key key = KeyFactory.createKey(Project.class.getSimpleName(), id);
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            Project project = pm.getObjectById(Project.class, key);
            long t1 = System.currentTimeMillis();
            pm.deletePersistent(project);
            return (int) (System.currentTimeMillis() - t1);
        } finally {
            pm.close();
        }
    }
}
