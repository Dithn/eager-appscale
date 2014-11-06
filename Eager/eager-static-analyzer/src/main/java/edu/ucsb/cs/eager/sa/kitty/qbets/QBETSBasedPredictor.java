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

import edu.ucsb.cs.eager.sa.kitty.APICall;
import edu.ucsb.cs.eager.sa.kitty.MethodInfo;
import edu.ucsb.cs.eager.sa.kitty.Prediction;
import edu.ucsb.cs.eager.sa.kitty.PredictionConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QBETSBasedPredictor {

    public static void predict(PredictionConfig config, List<MethodInfo> methods) throws IOException {
        if (config.isAggregateTimeSeries()) {
            throw new NotImplementedException();
        }

        for (MethodInfo m : methods) {
            System.out.println(m.getName());
            for (int i = 0; i < m.getName().length(); i++) {
                System.out.print("=");
            }
            System.out.println();
            System.out.println("Total paths: " + m.getPaths().size());
            // TODO: read q,c from CLI
            System.out.println("Worst-case exec time: " + predictExecTime(m,
                    config.getBenchmarkDataSvc(), 0.95, 0.05));
            System.out.println();
        }
    }

    private static Prediction predictExecTime(MethodInfo method, String bmDataSvc,
                                        double quantile, double confidence) {

        List<List<APICall>> pathsOfInterest = new ArrayList<List<APICall>>();
        for (List<APICall> p : method.getPaths()) {
            if (p.size() == 1 && p.get(0).getName().equals("-- No API Calls --")) {
                continue;
            }
            pathsOfInterest.add(p);
        }

        if (pathsOfInterest.size() == 0) {
            return new Prediction("No paths with API calls found");
        }

        for (int i = 0; i < pathsOfInterest.size(); i++) {
            List<APICall> path = pathsOfInterest.get(i);
            Set<String> uniqueOps = new HashSet<String>();
            for (APICall call : path) {
                uniqueOps.add(call.getShortName());
            }

            JSONObject msg = new JSONObject();
            msg.put("quantile", quantile);
            msg.put("confidence", confidence);
            msg.put("operations", new JSONArray(uniqueOps));
            System.out.println(msg.toString());
        }

        return null;
    }

}
