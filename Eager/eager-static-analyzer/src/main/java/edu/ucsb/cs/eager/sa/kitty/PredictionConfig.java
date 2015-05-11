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

public class PredictionConfig {

    private SimulationConfig simulationConfig = null;

    private QBETSConfig qbetsConfig = null;

    /**
     * Path to an existing Cerebro trace file, from which the
     * program execution paths can be extracted.
     */
    private String traceFile;

    /**
     * Use the simple QBETS predictor, instead of the trace-based
     * predictor.
     */
    private boolean simplePredictor;

    private String cerebroClasspath;
    private String clazz;
    private boolean loadNecessaryClasses = true;
    private boolean wholeProgramMode;

    private String[] methods;
    private String[] excludedMethods;

    private String[] excludedAPIPatterns;

    private int maxEntities = 1000;

    private boolean hideOutput;

    public String getTraceFile() {
        return traceFile;
    }

    public void setTraceFile(String traceFile) {
        this.traceFile = traceFile;
    }

    public String getCerebroClasspath() {
        return cerebroClasspath;
    }

    public void setCerebroClasspath(String cerebroClasspath) {
        this.cerebroClasspath = cerebroClasspath;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public boolean isLoadNecessaryClasses() {
        return loadNecessaryClasses;
    }

    public void setLoadNecessaryClasses(boolean loadNecessaryClasses) {
        this.loadNecessaryClasses = loadNecessaryClasses;
    }

    public boolean isWholeProgramMode() {
        return wholeProgramMode;
    }

    public void setWholeProgramMode(boolean wholeProgramMode) {
        this.wholeProgramMode = wholeProgramMode;
    }

    public boolean isSimplePredictor() {
        return simplePredictor;
    }

    public void setSimplePredictor(boolean simplePredictor) {
        this.simplePredictor = simplePredictor;
    }

    public boolean isEnabledMethod(String method) {
        if (excludedMethods != null) {
            for (String m : excludedMethods) {
                if (m.equals(method)) {
                    return false;
                }
            }
        }
        if (methods == null || methods.length == 0) {
            return true;
        }
        for (String m : methods) {
            if (m.equals(method)) {
                return true;
            }
        }
        return false;
    }

    public String[] getMethods() {
        return methods;
    }

    public void setMethods(String[] methods) {
        this.methods = methods;
    }

    public void setExcludedMethods(String[] excludedMethods) {
        this.excludedMethods = excludedMethods;
    }

    public int getMaxEntities() {
        return maxEntities;
    }

    public void setMaxEntities(int maxEntities) {
        this.maxEntities = maxEntities;
    }

    public boolean isHideOutput() {
        return hideOutput;
    }

    public void setHideOutput(boolean hideOutput) {
        this.hideOutput = hideOutput;
    }

    public boolean isExcludedAPI(String apiCall) {
        if (excludedAPIPatterns != null) {
            for (String pattern : excludedAPIPatterns) {
                if (apiCall.contains(pattern)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setExcludedAPIPatterns(String[] excludedAPIPatterns) {
        this.excludedAPIPatterns = excludedAPIPatterns;
    }

    public SimulationConfig getSimulationConfig() {
        return simulationConfig;
    }

    public void setSimulationConfig(SimulationConfig simulationConfig) {
        this.simulationConfig = simulationConfig;
    }

    public QBETSConfig getQbetsConfig() {
        return qbetsConfig;
    }

    public void setQbetsConfig(QBETSConfig qbetsConfig) {
        this.qbetsConfig = qbetsConfig;
    }

    public void validate() throws PredictionConfigException {
        if (simulationConfig == null && qbetsConfig == null) {
            throw new PredictionConfigException("Either a simulation config or a QBETS config " +
                    "must be specified.");
        } else if (simulationConfig != null && qbetsConfig != null) {
            throw new PredictionConfigException("Both simulation config and QBETS config should " +
                    "not be specified.");
        } else if (traceFile == null && cerebroClasspath == null) {
            throw new PredictionConfigException("One of trace file path and Cerebro class path " +
                    "must be specified.");
        } else if (traceFile != null && cerebroClasspath != null) {
            throw new PredictionConfigException("Trace file and Cerebro class path should not " +
                    "specified together.");
        } else if (cerebroClasspath != null && clazz == null) {
            throw new PredictionConfigException("Class must be specified when Cerebro class " +
                    "path is provided.");
        } else if (qbetsConfig != null) {
            qbetsConfig.validate();
        }

        if (methods != null && excludedMethods != null) {
            for (String m : methods) {
                for (String em : excludedMethods) {
                    if (m.equals(em)) {
                        throw new PredictionConfigException("Included and excluded methods " +
                                "must not intersect");
                    }
                }
            }
        }
    }
}
