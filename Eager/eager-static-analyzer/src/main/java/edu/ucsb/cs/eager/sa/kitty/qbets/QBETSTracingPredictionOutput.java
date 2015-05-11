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

import edu.ucsb.cs.eager.sa.kitty.AbstractPredictionOutput;
import edu.ucsb.cs.eager.sa.kitty.MethodInfo;

import java.io.PrintStream;
import java.util.Map;

public class QBETSTracingPredictionOutput extends AbstractPredictionOutput<TraceAnalysisResultSet> {

    @Override
    public void write(PrintStream out) {
        for (Map.Entry<MethodInfo,TraceAnalysisResultSet> entry : results.entrySet()) {
            MethodInfo method = entry.getKey();
            printTitle(out, method.getName(), '=');

            for (int pathIndex = 0; pathIndex < entry.getValue().size(); pathIndex++) {
                PathResult pathResult = entry.getValue().get(pathIndex);
                writePathResult(out, method, pathResult, pathIndex);
            }
        }
    }

    private void writePathResult(PrintStream out, MethodInfo method,
                                 PathResult pathResult, int pathIndex) {
        printTitle(out, "Path: " + pathIndex, '-');
        println(out, "API Calls: " + pathResult.getPath());
        println(out, "");

        int failures = 0;
        println(out, "[trace][method][path] index p1 p2 current  success success_rate");
        for (int i = 0; i < pathResult.getResults().length; i++) {
            TraceAnalysisResult r = pathResult.getResults()[i];
            if (i > 0) {
                boolean success = r.sum < pathResult.getResults()[i - 1].approach2;
                if (!success) {
                    failures++;
                }
                double successRate = ((double)(i - failures) / i) * 100.0;
                println(out, String.format(
                        "[trace][%s][%d] %4d %d %4d %4d %4d  %-5s %4.4f",
                        method.getName(), pathIndex, i + pathResult.getMinIndex(), r.timestamp,
                        r.approach1, r.approach2, r.sum, success, successRate));
            } else {
                println(out, String.format(
                        "[trace][%s][%d] %4d %d %4d %4d %4d  %-5s %-7s",
                        method.getName(), pathIndex, i + pathResult.getMinIndex(), r.timestamp,
                        r.approach1, r.approach2, r.sum, "N/A", "N/A"));
            }
        }
    }
}
