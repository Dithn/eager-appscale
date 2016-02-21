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

package edu.ucsb.cs.elkagent;

import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DemoHttpServlet extends HttpServlet {

    private static final Gson gson = new Gson();

    private ElkAgent agent;

    @Override
    public void init() throws ServletException {
        super.init();
        this.agent = new ElkAgent("http://128.111.179.159:9200", "logstash-memory", "jvmheap");
    }

    @Override
    public void destroy() {
        agent.close();
    }

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req,
                          HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    private void process(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {

        DemoResponse output = new DemoResponse();
        output.setStatus("success");
        output.setDescription("all systems online");
        resp.setStatus(200);
        resp.setContentType("application/json");
        resp.getOutputStream().print(gson.toJson(output));
        agent.report();
    }
}
