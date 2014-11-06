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

public class APICall {

    private String name;

    public APICall(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        if (name.startsWith("com.google.appengine.api.")) {
            String api = name.split("\\.")[4];
            String op = name.substring(name.indexOf('#') + 1, name.indexOf('('));
            return "bm_" + api + "_" + op;
        }
        throw new RuntimeException("Unsupported API call name: " + name);
    }
}
