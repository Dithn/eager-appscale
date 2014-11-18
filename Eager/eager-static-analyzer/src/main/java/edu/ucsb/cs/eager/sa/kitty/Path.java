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

import java.util.*;

public class Path {

    private List<APICall> calls = new ArrayList<APICall>();

    public void add(APICall call) {
        calls.add(call);
    }

    public List<APICall> calls() {
        return Collections.unmodifiableList(calls);
    }

    public int size() {
        return calls.size();
    }

    public boolean equivalent(Path p) {
        if (this.size() != p.size()) {
            return false;
        }

        Set<Integer> indexSet = new HashSet<Integer>();
        for (APICall call : this.calls) {
            boolean matchFound = false;
            for (int i = 0; i < p.calls.size(); i++) {
                if (call.equals(p.calls.get(i))) {
                    if (!indexSet.contains(i)) {
                        indexSet.add(i);
                        matchFound = true;
                        break;
                    }
                }
            }
            if (!matchFound) {
                return false;
            }
        }
        return true;
    }

}
