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

package edu.ucsb.cs;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;

public class InMemoryStudentServlet extends HttpServlet {

    private static final String OUTPUT = "{\"firstName\":\"%s\",\"lastName\":\"%s\",\"studentId\":\"%s\"}";

    @Override
    protected void doPost(HttpServletRequest req,
                          HttpServletResponse resp) throws ServletException, IOException {
        String studentId = UUID.randomUUID().toString();
        Student s = new Student();
        s.setFirstName(req.getParameter("firstName"));
        s.setLastName(req.getParameter("lastName"));
        s.setStudentId(studentId);
        StudentDB.getInstance().setStudent(studentId, s);

        resp.setStatus(201);
        resp.addHeader("Location", URI.create("/" + studentId).toString());
        sendJSON(s, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {

        long start = System.currentTimeMillis();
        String studentId = req.getParameter("studentId");
        try {
            Student s = StudentDB.getInstance().getStudent(studentId);
            if (s != null) {
                sendJSON(s, resp);
            } else {
                resp.sendError(404);
            }
        } finally {
            long end = System.currentTimeMillis();
            long total = end - start;
            TimeValues.getInstance().setTimes(0, total);
        }
    }

    private void sendJSON(Student s, HttpServletResponse resp) throws IOException {
        String output = String.format(OUTPUT, s.getFirstName(),
                s.getLastName(), s.getStudentId());
        resp.setContentType("application/json");
        resp.getOutputStream().print(output);
    }
}
