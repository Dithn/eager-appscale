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

package edu.ucsb.cs.eager.sa.kitty;

public class APICall implements Identifiable {

    private String name;
    private int iterations;

    private static final String[] loops = new String[]{
            "bm_datastore_asList",
    };

    public APICall(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public String getId() {
        if (name.startsWith("com.google.appengine.api.")) {
            String api = name.split("\\.")[4];
            String op = name.substring(name.indexOf('#') + 1, name.indexOf('('));
            String id = "bm_" + api + "_" + op;
            if (iterations > 0) {
                id += "_" + iterations;
            }
            return id;
        } else if (name.startsWith("edu.ucsb.cs.eager.gae.")) {
            // For testing...
            String op = name.substring(name.indexOf('#') + 1, name.indexOf('('));
            return "bm_test_datastore_" + op;
        }
        throw new RuntimeException("Unsupported API call name: " + name);
    }

    public boolean isLoop() {
        String id = getId();
        for (String l : loops) {
            if (l.equals(id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof APICall) && this.name.equals(((APICall) obj).name);
    }
}
