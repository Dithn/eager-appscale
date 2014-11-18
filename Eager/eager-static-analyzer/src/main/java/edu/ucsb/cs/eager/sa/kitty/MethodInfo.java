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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MethodInfo {

    private String name;

    /**
     * This is used to verify that the trace log parser picks up
     * all the captured paths correctly (for sanity checking purposes).
     */
    private int explicitPathCount;

    private List<Path> paths = new ArrayList<Path>();

    public MethodInfo(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addPath(Path path) {
        paths.add(path);
    }

    public List<Path> getPaths() {
        return paths;
    }

    public int getExplicitPathCount() {
        return explicitPathCount;
    }

    public void setExplicitPathCount(int explicitPathCount) {
        this.explicitPathCount = explicitPathCount;
    }

    @Override
    public String toString() {
        return name + ": " + paths.size() + " paths";
    }

    public static class MethodInfoComparator implements Comparator<MethodInfo> {
        @Override
        public int compare(MethodInfo o1, MethodInfo o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

}
