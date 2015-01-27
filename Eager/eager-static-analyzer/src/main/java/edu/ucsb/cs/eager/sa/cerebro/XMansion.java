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

package edu.ucsb.cs.eager.sa.cerebro;

import soot.SootMethod;

import java.util.*;

/**
 * A cache for storing intermediate method analysis results.
 */
public class XMansion {

    private final Map<SootMethod,CFGAnalyzer> cache = new HashMap<>();
    private final Set<SootMethod> calledMethods = new HashSet<>();

    public CFGAnalyzer getAnalyzer(SootMethod method) {
        if (cache.containsKey(method)) {
            return cache.get(method);
        } else if (calledMethods.contains(method)) {
            // recursion defense
            return null;
        } else if (!method.isConcrete()) {
            return null;
        }

        calledMethods.add(method);
        CFGAnalyzer analyzer = new CFGAnalyzer(method, this);
        cache.put(method, analyzer);
        calledMethods.remove(method);
        return analyzer;
    }

    public Map<SootMethod,CFGAnalyzer> getResults() {
        return Collections.unmodifiableMap(cache);
    }

}
