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

import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class BackupServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {

        Map<Long,Map<String,Integer>> results = new TreeMap<Long, Map<String, Integer>>();
        for (DataPoint p : DataPoint.getAll()) {
            results.put(p.getTimestamp(), p.getData());
        }
        JSONUtils.serializeMap(results, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {

        InputStream in = req.getInputStream();
        StringBuilder sb = new StringBuilder();
        byte[] data = new byte[1024];
        int len;
        while ((len = in.read(data)) != -1) {
            sb.append(new String(data, 0, len));
        }
        try {
            JSONObject obj = new JSONObject(sb.toString());
            Iterator keys = obj.keys();
            List<DataPoint> dataPoints = new ArrayList<DataPoint>();
            while (keys.hasNext()) {
                String timestamp = (String) keys.next();
                System.out.println("Timestamp: " + timestamp);
                DataPoint p = new DataPoint(Long.parseLong(timestamp));

                JSONObject child = obj.getJSONObject(timestamp);
                Iterator childKeys = child.keys();
                while (childKeys.hasNext()) {
                    String key = (String) childKeys.next();
                    p.put(key, child.getInt(key));
                }
                dataPoints.add(p);
            }

            if (DataPoint.restore(dataPoints)) {
                System.out.println("Restored " + dataPoints.size() + " data points...");
            } else {
                resp.sendError(500, "Restoration operation failed");
            }
        } catch (JSONException e) {
            throw new IOException("Error parsing JSON string", e);
        }
    }
}
