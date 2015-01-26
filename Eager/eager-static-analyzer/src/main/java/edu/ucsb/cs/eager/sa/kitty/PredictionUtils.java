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

import org.apache.commons.cli.*;

import java.util.ArrayList;
import java.util.List;

public class PredictionUtils {

    public static Prediction max(Prediction[] predictions) {
        Prediction max = new Prediction(0.0);
        for (Prediction prediction : predictions) {
            if (prediction.getValue() > max.getValue()) {
                max = prediction;
            }
        }
        return max;
    }

    public static List<Path> getPathsOfInterest(MethodInfo method) {
        List<Path> pathsOfInterest = new ArrayList<>();
        for (Path p : method.getPaths()) {
            if (p.size() == 1 && p.calls().get(0).getName().equals("-- No API Calls --")) {
                // this is for when the trace is loaded from a file
                continue;
            } else if (p.size() == 0) {
                // this is for when the trace is loaded from Cerebro
                continue;
            }
            pathsOfInterest.add(p);
        }
        return pathsOfInterest;
    }

    public static String pluralize(int count, String singular) {
        return count == 1 ? singular : singular + "s";
    }

    public static CommandLine parseCommandLineArgs(Options options, String[] args, String cmd) {
        try {
            CommandLineParser parser = new BasicParser();
            return parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage() + "\n");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(cmd, options);
            System.exit(1);
            return null;
        }
    }

    public static void addOption(Options options, String shortName, String longName,
                                 boolean hasArg, String desc) {
        if (options.getOption(shortName) != null || options.getOption(longName) != null) {
            throw new IllegalArgumentException("Duplicate argument: " + shortName + ", " + longName);
        }
        options.addOption(shortName, longName, hasArg, desc);
    }
}
