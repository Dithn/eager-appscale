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

import edu.ucsb.cs.eager.sa.kitty.MethodInfo;
import edu.ucsb.cs.eager.sa.kitty.PredictionConfig;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.util.List;

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

    private static double predictExecTime(MethodInfo m, String bmDataSvc,
                                        double quantile, double confidence) {
        return 0.0;
    }

}
