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

package edu.ucsb.cs.eager.sa.kitty.simulation;

import edu.ucsb.cs.eager.sa.kitty.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

public class SimulationBasedPredictor {

    public static void predict(PredictionConfig config, Collection<MethodInfo> methods) throws IOException {
        Map<String,TimingDistribution> benchmarkResults = loadBenchmarkDataFromDir(
                config.getBenchmarkDataDir());
        System.out.println("Running " + config.getSimulations() + " simulations...\n");

        for (MethodInfo m : methods) {
            System.out.println(m.getName());
            for (int i = 0; i < m.getName().length(); i++) {
                System.out.print("=");
            }
            System.out.println();
            System.out.println("Total paths: " + m.getPaths().size());
            System.out.println("Worst-case exec time: " + predictExecTime(m,
                    benchmarkResults, config.getSimulations()));
            System.out.println();
        }
    }

    private static Map<String,TimingDistribution> loadBenchmarkDataFromDir(
            String benchmarkDir) throws IOException {
        File dir = new File(benchmarkDir);
        File[] dataFiles = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".txt");
            }
        });
        Map<String,TimingDistribution> benchmarkResults = new HashMap<>();
        if (dataFiles.length > 0) {
            for (File f : dataFiles) {
                TimingDistribution dist = new TimingDistribution(f);
                benchmarkResults.put(dist.getApiCall(), dist);
            }
            System.out.println();
        }
        return benchmarkResults;
    }

    private static Prediction predictExecTime(MethodInfo method, Map<String,TimingDistribution> bm,
                                       int simulations) {
        List<Path> pathsOfInterest = PredictionUtils.getPathsOfInterest(method);
        if (pathsOfInterest.size() == 0) {
            return new Prediction("No paths with API calls found");
        }

        Prediction[] predictions = new Prediction[pathsOfInterest.size()];
        // Simulate each path
        for (int i = 0; i < pathsOfInterest.size(); i++) {
            predictions[i] = simulatePath(pathsOfInterest.get(i), bm, simulations);
        }
        // And return the most expensive one
        return PredictionUtils.max(predictions);
    }

    private static Prediction simulatePath(Path path, Map<String,TimingDistribution> bm,
                                    int simulations) {
        double[] results = new double[simulations];
        for (int i = 0; i < simulations; i++) {
            double total = 0.0;
            for (APICall call : path.calls()) {
                if (bm.containsKey(call.getId())) {
                    total += bm.get(call.getId()).sample();
                } else {
                    throw new RuntimeException("No benchmark data available for: " + call.getName());
                }
            }
            results[i] = total;
        }

        double total = 0.0;
        for (double r : results) {
            total += r;
        }
        return new Prediction(total / simulations);
    }

}
