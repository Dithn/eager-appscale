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

package edu.ucsb.cs.eager.sa.cerebro;

import org.apache.commons.cli.*;
import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.InvokeExpr;
import soot.jimple.toolkits.annotation.logic.Loop;

import java.io.File;
import java.util.*;

/**
 * Cerebro performs static analysis on a specified Java application.
 */
public class Cerebro {

    private Set<SootMethod> analyzedMethods = new HashSet<SootMethod>();
    private boolean loadNecessaryClasses = true;
    private boolean wholeProgramMode = false;
    private boolean verbose = false;
    private String classPath;
    private String starterClass;

    public Cerebro(String classPath, String starterClass) {
        this.classPath = classPath;
        this.starterClass = starterClass;
    }

    public void cleanup() {
        G.v().reset();
    }

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("ccp", "cerebro-classpath", true, "Cerebro classpath");
        options.addOption("c", "class", true, "Class to be used as the starting point");
        options.addOption("dnc", "disable-nec-classes", false, "Disable loading of necessary classes");
        options.addOption("wp", "whole-program", false, "Enable whole program mode");

        CommandLine cmd;
        try {
            CommandLineParser parser = new BasicParser();
            cmd = parser.parse( options, args);
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage() + "\n");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Cerebro", options);
            return;
        }

        String classPath = cmd.getOptionValue("ccp");
        if (classPath == null) {
            System.err.println("Cerebro classpath (ccp) option is required");
            return;
        } else {
            String[] paths = classPath.split(":");
            for (String p : paths) {
                File file = new File(p);
                if (!file.exists()) {
                    System.err.println("Path segment: " + p + " mentioned in classpath does not exist");
                    return;
                }
            }
        }

        String startingPoint = cmd.getOptionValue("c");
        if (startingPoint == null) {
            System.err.println("Starting point class (c) option is required");
            return;
        }

        Cerebro cerebro = new Cerebro(classPath, startingPoint);
        cerebro.setLoadNecessaryClasses(!cmd.hasOption("dnc"));
        cerebro.setWholeProgramMode(cmd.hasOption("wp"));
        cerebro.setVerbose(true);
        cerebro.analyze();
    }

    public Map<SootMethod,CFGAnalyzer> analyze() {
        XMansion xmansion = new XMansion();

        soot.options.Options.v().set_allow_phantom_refs(true);
        if (wholeProgramMode) {
            soot.options.Options.v().set_whole_program(true);
        }

        Scene.v().setSootClassPath(Scene.v().getSootClassPath() + ":" + classPath);
        SootClass clazz = Scene.v().loadClassAndSupport(starterClass);
        if (loadNecessaryClasses) {
            Scene.v().loadNecessaryClasses();
        }

        for (SootMethod method : clazz.getMethods()) {
            if (method.isPublic() || method.isProtected()) {
                analyzeMethod(method, xmansion);
            }
        }
        return xmansion.getResults();
    }

    private void analyzeMethod(SootMethod method, XMansion xmansion) {
        if (analyzedMethods.contains(method)) {
            return;
        }
        analyzedMethods.add(method);

        CFGAnalyzer analyzer = xmansion.getAnalyzer(method);
        printResult(method, analyzer);

        // Analyzing the previous method will generally cause more methods to be
        // analyzed, whose results will be added to the XMansion. So here we iterate
        // through all the results present in XMansion, and print them out.
        Map<SootMethod,CFGAnalyzer> results = xmansion.getResults();
        for (Map.Entry<SootMethod,CFGAnalyzer> entry : results.entrySet()) {
            if (analyzedMethods.contains(entry.getKey())) {
                continue;
            }
            analyzedMethods.add(entry.getKey());
            printResult(entry.getKey(), entry.getValue());
        }
    }

    void printResult(SootMethod method, CFGAnalyzer analyzer) {
        if (!verbose) {
            return;
        }
        String msg = "\nAnalyzing: " + method.getDeclaringClass().getName() + "#" +
                method.getName() + "()";
        System.out.println(msg);
        for (int i = 0; i < msg.length(); i++) {
            System.out.print("=");
        }
        System.out.println();

        Collection<Integer> pathApiCalls = analyzer.getPathApiCalls();
        Collection<Integer> pathAllocations = analyzer.getPathAllocations();
        System.out.println("Distinct paths through the code: " + pathApiCalls.size());
        System.out.print("API calls in paths: [");
        for (int count : pathApiCalls) {
            System.out.print(" " + count);
        }
        System.out.println(" ]");

        System.out.print("Memory (heap) allocations in paths: [");
        for (int count : pathAllocations) {
            System.out.print(" " + count);
        }
        System.out.println(" ]");

        Map<Loop,Integer> loopedApiCalls = analyzer.getLoopedApiCalls();
        Map<Loop,Integer> loopNestingLevels = analyzer.getLoopNestingLevels();
        System.out.println("Loops: " + loopedApiCalls.size());
        if (loopedApiCalls.size() > 0) {
            System.out.println("API calls in loops: ");
            for (Map.Entry<Loop,Integer> entry : loopedApiCalls.entrySet()) {
                System.out.println("  " + entry.getKey().getHead() + " [Nesting Level: " +
                        loopNestingLevels.get(entry.getKey()) + "] : " + entry.getValue());
            }
        }

        Collection<SootMethod> calledUserMethods = analyzer.getUserMethodCalls();
        if (calledUserMethods.size() > 0) {
            System.out.println("Called user-defined methods (unique): ");
            for (SootMethod calledMethod : calledUserMethods) {
                System.out.println("  " + calledMethod.getDeclaringClass().getName() + "#" +
                        calledMethod.getName() + "()");
            }
        }

        Collection<InvokeExpr> apiCalls = analyzer.getApiCalls();
        if (apiCalls.size() > 0) {
            System.out.println("Called GAE methods (non-unique): ");
            for (InvokeExpr call : apiCalls) {
                SootMethod calledMethod = call.getMethod();
                System.out.println("  [cloud] " + calledMethod.getDeclaringClass().getName() + "#" +
                        calledMethod.getName() + "()");
            }
        }

        Collection<List<SootMethod>> paths = analyzer.getPaths();
        if (paths.size() > 0) {
            System.out.println("API call traces: ");
            int i = 0;
            for (List<SootMethod> path : paths) {
                if (path.size() == 0) {
                    System.out.println("  [path" + i + "] -- No API Calls --");
                } else {
                    for (SootMethod m : path) {
                        System.out.println("  [path" + i + "] " + m.getDeclaringClass().getName() + "#" +
                                m.getName() + "()");
                    }
                }
                i++;
            }
        }
    }

    public void setLoadNecessaryClasses(boolean loadNecessaryClasses) {
        this.loadNecessaryClasses = loadNecessaryClasses;
    }

    public void setWholeProgramMode(boolean wholeProgramMode) {
        this.wholeProgramMode = wholeProgramMode;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
