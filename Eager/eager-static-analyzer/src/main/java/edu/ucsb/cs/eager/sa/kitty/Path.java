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

public class Path implements Identifiable {

    private List<APICall> calls = new ArrayList<>();

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

        Set<Integer> indexSet = new HashSet<>();
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

    public String getId() {
        List<String> names = new ArrayList<>();
        for (APICall call : calls) {
            names.add(call.getId());
        }
        Collections.sort(names);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                sb.append(':');
            }
            sb.append(names.get(i));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        if (calls.size() == 0) {
            return "[empty]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(calls.get(0).getId());
        for (int i = 1; i < calls.size(); i++) {
            sb.append(" -> ").append(calls.get(i).getId());
        }
        return sb.toString();
    }
}
