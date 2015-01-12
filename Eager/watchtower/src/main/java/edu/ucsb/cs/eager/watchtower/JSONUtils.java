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

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.labs.repackaged.org.json.JSONArray;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JSONUtils {

    public static void serialize(Map<String,Map<String,Integer>> map,
                                 HttpServletResponse response) throws IOException {
        JSONObject json = new JSONObject(map);
        setContentType(response);
        response.getOutputStream().println(json.toString());
    }

    public static void serializeQueryResult(Map<String,List<Integer>> map,
                                 HttpServletResponse response) throws IOException {
        JSONObject json = new JSONObject(map);
        setContentType(response);
        response.getOutputStream().println(json.toString());
    }

    public static void serializeDump(Map<Long,Map<String,Integer>> map,
                                      HttpServletResponse response) throws IOException {
        JSONObject json = new JSONObject(map);
        setContentType(response);
        response.getOutputStream().println(json.toString());
    }

    public static void serializeCollectionStatus(boolean collectionStopped,
                                                 HttpServletResponse response) throws IOException {
        try {
            JSONObject json = new JSONObject();
            json.put("collectionStopped", collectionStopped);
            response.getOutputStream().println(json.toString());
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    public static void sendStatusMessage(String message,
                                         HttpServletResponse response) throws IOException {
        try {
            JSONObject json = new JSONObject();
            json.put("message", message);
            response.getOutputStream().println(json.toString());
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    private static void setContentType(HttpServletResponse response) {
        response.setContentType("application/json");
    }
}
