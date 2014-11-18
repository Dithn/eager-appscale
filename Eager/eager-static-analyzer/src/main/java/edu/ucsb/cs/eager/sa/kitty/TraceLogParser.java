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

public class TraceLogParser {

    private static final int READY = 0;
    private static final int METHOD = 1;
    private static final int PATHS = 2;

    private int state = READY;
    private MethodInfo currentMethod;
    private String currentPrefix;
    private Path currentPath;

    private Set<MethodInfo> methods = new TreeSet<MethodInfo>(new MethodInfo.MethodInfoComparator());

    public void parse(String line) {
        if (line.startsWith("Warning: ")) {
            return;
        }
        switch (state) {
            case READY:
                if (line.startsWith("Analyzing: ")) {
                    state = METHOD;
                    currentMethod = new MethodInfo(line.substring(line.indexOf(' ') + 1));
                }
                break;

            case METHOD:
                if (line.startsWith("API call traces:")) {
                    state = PATHS;
                    currentPrefix = "[path0]";
                    currentPath = new Path();
                } else if (line.startsWith("Distinct paths through the code: ")) {
                    String pathCountStr = line.substring(line.indexOf(':') + 2);
                    int pathCount = Integer.parseInt(pathCountStr);
                    currentMethod.setExplicitPathCount(pathCount);
                }
                break;

            case PATHS:
                if ("".equals(line)) {
                    state = READY;
                    currentMethod.addPath(currentPath);
                    if (currentMethod.getExplicitPathCount() != currentMethod.getPaths().size()) {
                        throw new RuntimeException("Invalid path count. Want: " +
                                currentMethod.getExplicitPathCount() + "; Got: " +
                                currentMethod.getPaths().size() + "; Method: " + currentMethod);
                    }
                    methods.add(currentMethod);
                } else {
                    line = line.trim();
                    int index = line.indexOf(' ');
                    String prefix = line.substring(0, index);
                    String apiCall = line.substring(index + 1);
                    if (!prefix.equals(currentPrefix)) {
                        currentMethod.addPath(currentPath);
                        currentPath = new Path();
                    }
                    currentPath.add(new APICall(apiCall));
                    currentPrefix = prefix;
                }
                break;
        }
    }

    public Collection<MethodInfo> getMethods() {
        return methods;
    }
}
