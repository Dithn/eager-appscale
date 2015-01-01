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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DatastoreBenchmarkServlet extends HttpServlet {

    private static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {

        String op = req.getParameter("op");
        String id = req.getParameter("id");
        if ("get".equals(op)) {
            Key key = KeyFactory.createKey(DatastoreBenchmark.PROJECT_KIND, id);
            try {
                datastore.get(key);
            } catch (EntityNotFoundException e) {
                throw new ServletException("Failed to locate object by key", e);
            }
        } else if ("put".equals(op)) {
            Entity project = new Entity(DatastoreBenchmark.PROJECT_KIND, id);
            project.setProperty(DatastoreBenchmark.PROJECT_ID, id);
            project.setProperty(DatastoreBenchmark.NAME, "Project-" + id);
            project.setProperty(DatastoreBenchmark.DESCRIPTION, "Description");
            project.setProperty(DatastoreBenchmark.LICENSE, "License");
            datastore.put(project);
        } else if ("delete".equals(op)) {
            Key key = KeyFactory.createKey(DatastoreBenchmark.PROJECT_KIND, id);
            datastore.delete(key);
        }
        resp.getOutputStream().println("Done: " + op);
    }
}
