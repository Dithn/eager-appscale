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

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import edu.ucsb.cs.eager.watchtower.Constants;

import javax.servlet.ServletException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MemcacheBenchmark extends APIBenchmark {

    @Override
    public String getName() {
        return "Memcache";
    }

    @Override
    public Map<String, Integer> benchmark() throws ServletException {
        MemcacheService cache = MemcacheServiceFactory.getMemcacheService();
        String key = "TetKey_" + UUID.randomUUID();
        Map<String,Integer> results = new HashMap<String, Integer>();

        results.put(Constants.Memcache.PUT, put(cache, key));
        sleep(100);

        results.put(Constants.Memcache.GET, get(cache, key));
        sleep(100);

        results.put(Constants.Memcache.DELETE, delete(cache, key));
        return results;
    }

    private int put(MemcacheService cache, String key) {
        long start = System.currentTimeMillis();
        cache.put(key, "TestValue");
        return (int) (System.currentTimeMillis() - start);
    }

    private int get(MemcacheService cache, String key) {
        long start = System.currentTimeMillis();
        cache.get(key);
        return (int) (System.currentTimeMillis() - start);
    }

    private int delete(MemcacheService cache, String key) {
        long start = System.currentTimeMillis();
        cache.delete(key);
        return (int) (System.currentTimeMillis() - start);
    }
}
