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

import edu.ucsb.cs.eager.sa.branches.RandomBranchSelector;
import edu.ucsb.cs.eager.sa.branches.tlat.SaturatingCounterPattern;
import edu.ucsb.cs.eager.sa.branches.tlat.TakeLastPattern;
import edu.ucsb.cs.eager.sa.branches.tlat.TwoLevelAdaptiveBranchSelector;
import junit.framework.TestCase;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class AdvancedSimulatorTest extends TestCase {

    public void testPerformanceSimulatorWithRandom() {
        PerformanceSimulator simulator = new PerformanceSimulator(new RandomBranchSelector());
        simulator.addUserPackage("net.eager.testing");
        simulator.addSpecialPackage("edu.ucsb.cs.eager.gae");

        UnitGraph g = getUnitGraph();
        SimulationManager manager = new SimulationManager(g, simulator);
        double result = manager.simulate(1000, false);
        System.out.println("Average performance (Random) = " + result);
    }

    public void testPerformanceSimulatorWithTLATCounter() {
        PerformanceSimulator simulator = new PerformanceSimulator(
                new TwoLevelAdaptiveBranchSelector(new SaturatingCounterPattern.SaturatingCounterPatternFactory()));
        simulator.addUserPackage("net.eager.testing");
        simulator.addSpecialPackage("edu.ucsb.cs.eager.gae");

        UnitGraph g = getUnitGraph();
        SimulationManager manager = new SimulationManager(g, simulator);
        double result = manager.simulate(1000, false);
        System.out.println("Average performance (TLAT-Counter) = " + result);
    }

    public void testPerformanceSimulatorWithTLATTakeLast() {
        PerformanceSimulator simulator = new PerformanceSimulator(
                new TwoLevelAdaptiveBranchSelector(new TakeLastPattern.TakeLastPatternFactory()));
        simulator.addUserPackage("net.eager.testing");
        simulator.addSpecialPackage("edu.ucsb.cs.eager.gae");

        UnitGraph g = getUnitGraph();
        SimulationManager manager = new SimulationManager(g, simulator);
        double result = manager.simulate(1000, false);
        System.out.println("Average performance (TLAT-TakeLast) = " + result);
    }

    private UnitGraph getUnitGraph() {
        SootClass c = Scene.v().loadClassAndSupport("net.eager.testing.TestClass");
        c.setApplicationClass();
        SootMethod m = c.getMethodByName("main");
        Body b = m.retrieveActiveBody();
        return new BriefUnitGraph(b);
    }


}
