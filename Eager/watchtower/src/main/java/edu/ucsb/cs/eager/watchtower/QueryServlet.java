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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {
        String opsParam = req.getParameter("ops");
        String startParam = req.getParameter("start");
        String endParam = req.getParameter("end");

        String[] ops = new String[]{};
        if (opsParam != null && !"".equals(opsParam)) {
            ops = opsParam.trim().split(",");
        }

        long start = -1L, end = Long.MAX_VALUE;
        if (startParam != null) {
            start = Long.parseLong(startParam);
        }
        if (endParam != null) {
            end = Long.parseLong(endParam);
        }

        Map<String,List<Integer>> results = new HashMap<String, List<Integer>>();
        for (DataPoint p : DataPoint.getAll()) {
            if (start <= p.getTimestamp() && p.getTimestamp() <= end) {
                Map<String,Integer> data = p.getData();
                for (Map.Entry<String,Integer> entry : data.entrySet()) {
                    if (addToResult(entry.getKey(), ops)) {
                        addToMap(results, entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        JSONUtils.serializeQueryResult(results, resp);
    }

    private boolean addToResult(String key, String[] ops) {
        if (ops.length > 0) {
            for (String op : ops) {
                if (op.equals(key)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private void addToMap(Map<String,List<Integer>> map, String key, int value) {
        if (!map.containsKey(key)) {
            map.put(key, new ArrayList<Integer>());
        }
        map.get(key).add(value);
    }
}
