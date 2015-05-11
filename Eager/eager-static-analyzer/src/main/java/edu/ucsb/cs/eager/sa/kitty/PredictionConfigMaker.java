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

import edu.ucsb.cs.eager.sa.kitty.qbets.QBETSConfig;
import edu.ucsb.cs.eager.sa.kitty.simulation.SimulationConfig;
import org.apache.commons.cli.*;

public class PredictionConfigMaker {

    private CommandLine cmd;

    protected void preConstruction(Options options) {
    }

    protected void postConstruction(PredictionConfig config) throws PredictionConfigException {
    }

    final public PredictionConfig construct(String[] args,
                                            String command) throws PredictionConfigException {
        Options options = getOptions();
        preConstruction(options);
        cmd = parseCommandLineArgs(options, args, command);
        PredictionConfig config = getPredictionConfig(cmd);
        postConstruction(config);
        return config;
    }

    final public String getOptionValue(String op) {
        if (cmd == null) {
            throw new IllegalStateException("CommandLine not initialized");
        }
        return cmd.getOptionValue(op);
    }

    final public boolean hasOption(String op) {
        if (cmd == null) {
            throw new IllegalStateException("CommandLine not initialized");
        }
        return cmd.hasOption(op);
    }

    private Options getOptions() {
        Options options = new Options();
        addOption(options, "i", "input-file", true,
                "Path to the Cerebro trace file");

        // Soot configuration
        addOption(options, "ccp", "cerebro-classpath", true, "Cerebro classpath");
        addOption(options, "c", "class", true, "Class to be used as the starting point");
        addOption(options, "dnc", "disable-nec-classes", false, "Disable loading of necessary classes");
        addOption(options, "wp", "whole-program", false, "Enable whole program mode");
        addOption(options, "m", "methods", true, "Methods that should be analyzed");
        addOption(options, "ex", "excluded-methods", true, "Methods that should be excluded from the analysis");

        // SimulationConfig
        addOption(options, "b", "benchmark-dir", true, "Path to the directory containing seed benchmark results");
        addOption(options, "sn", "simulations", true, "Number of times to simulate each path (default 100)");

        // QBETSConfig
        addOption(options, "s", "benchmark-svc", true, "URL of the benchmark data service");
        addOption(options, "q", "quantile", true, "Execution time quantile that should be predicted");
        addOption(options, "cn", "confidence", true, "Upper confidence of the predicted execution time quantile");
        addOption(options, "a", "aggregate-ts", false, "Aggregate multiple time series into a single time series");
        addOption(options, "st", "start", true, "Start timestamp for fetching time series data");
        addOption(options, "en", "end", true, "End timestamp for fetching time series data");
        addOption(options, "d1", "disable-approach1", false, "Disable computing approach1 predictions");

        addOption(options, "sp", "simple", false, "Use the simple QBETS predictor");
        addOption(options, "me", "max-entities", true, "Maximum entities that may exist in the datastore");
        addOption(options, "ea", "excluded-apis", true, "GAE APIs that should be excluded from the analysis");

        return options;
    }

    private PredictionConfig getPredictionConfig(CommandLine cmd) throws PredictionConfigException {
        PredictionConfig config = new PredictionConfig();
        config.setTraceFile(cmd.getOptionValue("i"));
        config.setCerebroClasspath(cmd.getOptionValue("ccp"));
        config.setClazz(cmd.getOptionValue("c"));
        config.setLoadNecessaryClasses(!cmd.hasOption("dnc"));
        config.setWholeProgramMode(cmd.hasOption("wp"));

        String methods = cmd.getOptionValue("m");
        if (methods != null) {
            config.setMethods(methods.split(","));
        }

        String excludedMethods = cmd.getOptionValue("ex");
        if (excludedMethods != null) {
            config.setExcludedMethods(excludedMethods.split(","));
        }

        String benchmarkDataDir = cmd.getOptionValue("b");
        if (benchmarkDataDir != null) {
            SimulationConfig simulationConfig = new SimulationConfig();
            simulationConfig.setBenchmarkDataDir(benchmarkDataDir);
            String sn = cmd.getOptionValue("sn");
            if (sn != null) {
                simulationConfig.setSimulations(Integer.parseInt(sn));
            }

            config.setSimulationConfig(simulationConfig);
        }

        String benchmarkDataSvc = cmd.getOptionValue("s");
        if (benchmarkDataSvc != null) {
            QBETSConfig qbetsConfig = new QBETSConfig();
            qbetsConfig.setBenchmarkDataSvc(benchmarkDataSvc);
            String q = cmd.getOptionValue("q");
            if (q != null) {
                qbetsConfig.setQuantile(Double.parseDouble(q));
            }
            String c = cmd.getOptionValue("cn");
            if (c != null) {
                qbetsConfig.setConfidence(Double.parseDouble(c));
            }
            String start = cmd.getOptionValue("st");
            if (start != null) {
                qbetsConfig.setStart(Long.parseLong(start));
            }
            String end = cmd.getOptionValue("en");
            if (end != null) {
                qbetsConfig.setEnd(Long.parseLong(end));
            }
            qbetsConfig.setDisableApproach1(cmd.hasOption("d1"));

            config.setQbetsConfig(qbetsConfig);
        }

        config.setSimplePredictor(cmd.hasOption("sp"));

        String maxEntities = cmd.getOptionValue("me");
        if (maxEntities != null) {
            config.setMaxEntities(Integer.parseInt(maxEntities));
        }

        String excludedAPIs = cmd.getOptionValue("ea");
        if (excludedAPIs != null) {
            config.setExcludedAPIPatterns(excludedAPIs.split(","));
        }

        config.validate();
        return config;
    }

    public static void addOption(Options options, String shortName, String longName,
                                 boolean hasArg, String desc) {
        if (options.getOption(shortName) != null || options.getOption(longName) != null) {
            throw new IllegalArgumentException("Duplicate argument: " + shortName + ", " + longName);
        }
        options.addOption(shortName, longName, hasArg, desc);
    }

    private CommandLine parseCommandLineArgs(Options options, String[] args, String cmd) {
        CommandLineParser parser = new BasicParser();
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage() + "\n");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(cmd, options);
            System.exit(1);
            return null;
        }
    }


}
