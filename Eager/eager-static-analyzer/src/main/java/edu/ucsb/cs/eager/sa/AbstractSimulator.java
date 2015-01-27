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

package edu.ucsb.cs.eager.sa;

import soot.SootMethod;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class AbstractSimulator implements InstructionSimulator {

    private Map<String,SimulationManager> cache = new HashMap<>();

    private Set<String> userPackages = new HashSet<>();
    private Set<String> specialPackages = new HashSet<>();

    private BranchSelector selector;

    protected AbstractSimulator(BranchSelector selector) {
        this.selector = selector;
    }

    public void addUserPackage(String pkg) {
        userPackages.add(pkg);
    }

    public void addSpecialPackage(String pkg) {
        specialPackages.add(pkg);
    }

    private SimulationManager getSimulationManager(SootMethod method) {
        String key = getKey(method);
        SimulationManager manager = cache.get(key);
        if (manager == null) {
            UnitGraph subGraph = new BriefUnitGraph(method.retrieveActiveBody());
            manager = new SimulationManager(subGraph, this);
            cache.put(key, manager);
        }
        return manager;
    }

    private String getKey(SootMethod method) {
        return method.getDeclaringClass().getName() + "#" + method.getName();
    }

    public double simulateInstruction(Stmt stmt) {
        if (stmt.containsInvokeExpr()) {
            InvokeExpr invocation = stmt.getInvokeExpr();
            SootMethod method = invocation.getMethod();
            String pkg = method.getDeclaringClass().getPackageName();
            if (userPackages.contains(pkg)) {
                // If it's a user-defined function, run the simulation recursively
                SimulationManager manager = getSimulationManager(method);
                return manager.simulate(1, false);
            } else if (specialPackages.contains(pkg)) {
                return simulateSpecialInvokeInstruction(invocation, method);
            }
            return simulateRegularInvokeInstruction(invocation, method);
        }
        return simulateNonInvokeInstruction(stmt);
    }

    /**
     * Simulate the invocation of a "special" method. Special methods are the methods that
     * belong to any of the special packages. Special packages can be defined by calling
     * the addSpecialPackage method.
     *
     * @param invocation InvokeExpr instruction
     * @param method SootMethod instance
     * @return a double value
     */
    protected abstract double simulateSpecialInvokeInstruction(InvokeExpr invocation, SootMethod method);

    /**
     * Simulate the invocation of a regular method. Any method that doesn't belong to a special
     * package and does not belong to any of the user packages is considered to be regular.
     *
     * @param invocation InvokeExpr instruction
     * @param method SootMethod instance
     * @return a double value
     */
    protected abstract double simulateRegularInvokeInstruction(InvokeExpr invocation, SootMethod method);

    /**
     * Simulate the execution of an instruction that is not a method call.
     *
     * @param stmt Instruction to be simulated
     * @return a double value
     */
    protected abstract double simulateNonInvokeInstruction(Stmt stmt);

    @Override
    public BranchSelector getBranchSelector() {
        return selector;
    }
}
