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

package edu.ucsb.cs.eager.sa.kitty.qbets;

import java.util.HashMap;
import java.util.Map;

public class QuantileCache {

    private Map<String,Integer> cache = new HashMap<>();

    public void put(String op, int pathLength, int quantile) {
        cache.put(key(op, pathLength), quantile);
    }

    public int get(String op, int pathLength) {
        String k = key(op, pathLength);
        if (cache.containsKey(k)) {
            return cache.get(k);
        }
        throw new IllegalArgumentException("No benchmark data available for: " + op);
    }

    public boolean contains(String op, int pathLength) {
        return cache.containsKey(key(op, pathLength));
    }

    private String key(String op, int pathLength) {
        return op + "__" + pathLength;
    }
}
