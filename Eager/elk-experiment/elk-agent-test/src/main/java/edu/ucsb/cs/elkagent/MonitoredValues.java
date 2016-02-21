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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public final class MonitoredValues {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    private static final Gson gson = new Gson();

    private final Map<String,Object> values = new HashMap<>();

    private MonitoredValues() {
    }

    public static MonitoredValues newInstance() {
        return new MonitoredValues();
    }

    public MonitoredValues with(String key, Object value) {
        values.put(key, value);
        return this;
    }

    public MonitoredValues with(Map<String,Object> values) {
        if (values != null) {
            this.values.putAll(values);
        }
        return this;
    }

    public MonitoredValues withTimestamp(String key) {
        values.put(key, dateFormat.format(new Date()));
        return this;
    }

    public MonitoredValues withMemoryUsageData() {
        Runtime runtime = Runtime.getRuntime();
        double total = runtime.totalMemory() / (1024.0 * 1024.0);
        double free = runtime.freeMemory() / (1024.0 * 1024.0);
        values.put("total", total);
        values.put("free", free);
        values.put("used", total - free);
        return this;
    }

    public String toJson() {
        return gson.toJson(values);
    }
}
