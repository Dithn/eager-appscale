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

package edu.ucsb.cs.eager.sa.loops;

import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.annotation.logic.LoopFinder;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;

import java.util.*;

/**
 * Based on abstract interpretation method from Stefan Bygde. See the dissertation
 * titled "Static WCET analysis based on abstract interpretation and counting of
 * elements".
 */
public class LoopBoundAnalysis {

    private Collection<Loop> loops;
    private final DirectedGraph<Stmt> graph;
    private boolean debug;
    private boolean done;

    private Map<Edge,ProgramState> states = new LinkedHashMap<>();
    private Set<Value> variables = new HashSet<>();
    private Map<Stmt,Set<Value>> loopInvariants = new HashMap<>();

    public LoopBoundAnalysis(Body body, boolean debug) {
        this(body);
        this.debug = debug;
    }

    public LoopBoundAnalysis(Body body) {
        graph = (DirectedGraph) new BriefUnitGraph(body);

        // Find all integer variables
        for (Stmt stmt : graph) {
            for (ValueBox value : stmt.getDefBoxes()) {
                Value v = value.getValue();
                if (v.getType() instanceof IntType) {
                    variables.add(v);
                }
            }
        }

        LoopFinder loopFinder = new LoopFinder();
        loopFinder.transform(body);
        loops = loopFinder.loops();

        // invariant analysis
        for (Loop loop : loops) {
            Set<Value> invariants = new HashSet<>(variables);
            for (Stmt stmt : loop.getLoopStatements()) {
                for (ValueBox value : stmt.getDefBoxes()) {
                    Value v = value.getValue();
                    if (v.getType() instanceof IntType) {
                        invariants.remove(v);
                    }
                }
            }
            loopInvariants.put(loop.getHead(), invariants);
        }
    }

    public void analyze() {
        if (loops.size() > 0) {
            findEdges(graph);
            computeStates();
        }
        done = true;
    }

    public Map<Loop,Integer> getLoopBounds() {
        if (!done) {
            analyze();
        }
        Map<Loop,Integer> bounds = new HashMap<>();
        for (Loop loop : loops) {
            IfStmt head = (IfStmt) loop.getHead();
            Set<Value> invariants = loopInvariants.get(head);
            Set<Value> loopControlVariables = new HashSet<>();
            for (ValueBox value : head.getUseBoxes()) {
                Value v = value.getValue();
                if (v instanceof JimpleLocal && v.getType() instanceof IntType &&
                        !invariants.contains(v)) {
                    loopControlVariables.add(v);
                }
            }

            List<Stmt> next = graph.getSuccsOf(head);
            for (Stmt stmt : next) {
                if (!stmt.equals(head.getTarget())) {
                    int result = 1;
                    ProgramState state = states.get(new Edge(head, stmt));
                    for (Value v : loopControlVariables) {
                        int count = state.get(v).getStates();
                        if (count > 0) {
                            result *= count;
                        } else {
                            result = -1;
                            break;
                        }
                    }
                    bounds.put(loop, result);
                    break;
                }
            }
        }
        return bounds;
    }

    private void computeStates() {
        while (true) {
            // Create a new state for this iteration
            Map<Edge,ProgramState> newStates = new LinkedHashMap<>();
            for (Map.Entry<Edge,ProgramState> entry : states.entrySet()) {
                ProgramState temp = null;
                if (entry.getValue() != null) {
                    temp = new ProgramState();
                    entry.getValue().copy(temp);
                }
                newStates.put(entry.getKey(), temp);
            }

            // Run the computation
            Stmt head = graph.getHeads().get(0);
            visitAndCompute(head, new HashSet<Stmt>(), newStates);

            // Check whether the algorithm has converged
            boolean stabilized = true;
            for (Map.Entry<Edge,ProgramState> entry : states.entrySet()) {
                if (!entry.getValue().equals(newStates.get(entry.getKey()))) {
                    stabilized = false;
                    break;
                }
            }

            if (debug) {
                for (Map.Entry<Edge,ProgramState> entry : newStates.entrySet()) {
                    System.out.println(entry.getKey().src + ": " + newStates.get(entry.getKey()));
                    System.out.flush();
                }
                System.out.println("\n\n");
                System.out.flush();
            }

            // Update the states
            states = newStates;
            if (stabilized) {
                break;
            }
        }
    }

    private void visitAndCompute(Stmt node, Set<Stmt> visitedNodes, Map<Edge,ProgramState> newStates) {
        if (visitedNodes.contains(node)) {
            return;
        }
        visitedNodes.add(node);

        for (Stmt child : graph.getSuccsOf(node)) {
            computeEdgeValue(node, child, newStates);
            visitAndCompute(child, visitedNodes, newStates);
        }
    }

    private void computeEdgeValue(Stmt src, Stmt dst, Map<Edge,ProgramState> newStates) {
        Edge outEdge = new Edge(src, dst);
        ProgramState out = newStates.get(outEdge);
        ProgramState in;

        List<Stmt> prev = graph.getPredsOf(src);
        if (prev.size() == 1) {
            in = newStates.get(new Edge(prev.get(0), src));
            in.copy(out);
        } else if (prev.size() == 2) {
            ProgramState in1 = newStates.get(new Edge(prev.get(0), src));
            ProgramState in2 = newStates.get(new Edge(prev.get(1), src));

            boolean widen = isLoopHead(src);
            ProgramState temp = new ProgramState();
            out.copy(temp);

            for (Value var : in1.getVariables()) {
                IntegerInterval interval1 = in1.get(var);
                IntegerInterval interval2 = in2.get(var);
                if (interval2 != null) {
                    out.updateState(var, interval1.sup(interval2));
                } else {
                    out.updateState(var, interval1);
                }
            }

            if (widen) {
                Set<Value> invariants = loopInvariants.get(src);
                for (Value var : temp.getVariables()) {
                    if (invariants.contains(var)) {
                        // restricted widening
                        continue;
                    }
                    IntegerInterval interval1 = temp.get(var);
                    IntegerInterval interval2 = out.get(var);
                    out.updateState(var, interval1.widen(interval2));
                }
            }
            in = out;
        } else if (prev.size() > 2) {
            throw new UnsupportedOperationException("no support for merging > 2 branches yet");
        } else {
            in = null;
        }

        if (src instanceof AssignStmt) {
            Value rightOp = ((AssignStmt) src).getRightOp();
            if (rightOp.getType() instanceof IntType) {
                Value leftOp = ((AssignStmt) src).getLeftOp();
                if (rightOp instanceof IntConstant) {
                    int value = ((IntConstant) rightOp).value;
                    out.updateState(leftOp, value, value);
                } else if (rightOp instanceof AddExpr) {
                    Value op1 = ((AddExpr) rightOp).getOp1();
                    Value op2 = ((AddExpr) rightOp).getOp2();
                    if (op1 instanceof JimpleLocal && op2 instanceof IntConstant) {
                        out.updateState(op1, out.get(op1).add(((IntConstant) op2).value));
                    }
                }
            }
        } else if (src instanceof IfStmt) {
            if (in == null) {
                throw new IllegalStateException("in set was null");
            }
            IfStmt ifStmt = (IfStmt) src;
            if (dst.equals(ifStmt.getTarget())) {
                Value condition = ifStmt.getCondition();
                if (condition instanceof GtExpr) {
                    Value op1 = ((GtExpr) condition).getOp1();
                    Value op2 = ((GtExpr) condition).getOp2();
                    if (op1 instanceof JimpleLocal && op2 instanceof IntConstant) {
                        IntegerInterval interval = in.get(op1);
                        out.updateState(op1, interval.gt(((IntConstant) op2).value));
                    } else if (op1 instanceof JimpleLocal && op2 instanceof JimpleLocal) {
                        IntegerInterval interval = in.get(op1);
                        out.updateState(op1, interval.gt(in.get(op2)));
                    }
                } else if (condition instanceof GeExpr) {
                    Value op1 = ((GeExpr) condition).getOp1();
                    Value op2 = ((GeExpr) condition).getOp2();
                    if (op1 instanceof JimpleLocal && op2 instanceof IntConstant) {
                        IntegerInterval interval = in.get(op1);
                        out.updateState(op1, interval.gte(((IntConstant) op2).value));
                    } else if (op1 instanceof JimpleLocal && op2 instanceof JimpleLocal) {
                        IntegerInterval interval = in.get(op1);
                        out.updateState(op1, interval.gte(in.get(op2)));
                    }
                }
            } else {
                Value condition = ifStmt.getCondition();
                if (condition instanceof GtExpr) {
                    Value op1 = ((GtExpr) condition).getOp1();
                    Value op2 = ((GtExpr) condition).getOp2();
                    if (op1 instanceof JimpleLocal && op2 instanceof IntConstant) {
                        IntegerInterval interval = in.get(op1);
                        out.updateState(op1, interval.lte(((IntConstant) op2).value));
                    } else if (op1 instanceof JimpleLocal && op2 instanceof JimpleLocal) {
                        IntegerInterval interval = in.get(op1);
                        out.updateState(op1, interval.lte(in.get(op2)));
                    }
                } else if (condition instanceof GeExpr) {
                    Value op1 = ((GeExpr) condition).getOp1();
                    Value op2 = ((GeExpr) condition).getOp2();
                    if (op1 instanceof JimpleLocal && op2 instanceof IntConstant) {
                        IntegerInterval interval = in.get(op1);
                        out.updateState(op1, interval.lt(((IntConstant) op2).value));
                    } else if (op1 instanceof JimpleLocal && op2 instanceof JimpleLocal) {
                        IntegerInterval interval = in.get(op1);
                        out.updateState(op1, interval.lt(in.get(op2)));
                    }
                }
            }
        }
    }

    private void findEdges(DirectedGraph<Stmt> graph) {
        states.clear();
        Set<Stmt> visitedNodes = new HashSet<>();
        Stmt head = graph.getHeads().get(0);
        visit(head, graph, visitedNodes);
    }

    private void visit(Stmt node, DirectedGraph<Stmt> graph, Set<Stmt> visitedNodes) {
        if (visitedNodes.contains(node)) {
            return;
        }
        visitedNodes.add(node);
        List<Stmt> children = graph.getSuccsOf(node);
        for (Stmt child : children) {
            if (graph.getHeads().get(0).equals(node)) {
                states.put(new Edge(node, child), newProgramState());
            } else {
                states.put(new Edge(node, child), new ProgramState());
            }
            visit(child, graph, visitedNodes);
        }
    }

    private ProgramState newProgramState() {
        ProgramState state = new ProgramState();
        for (Value v : variables) {
            state.updateState(v, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }
        return state;
    }

    public boolean isLoopHead(Stmt stmt) {
        for (Loop loop : loops) {
            if (loop.getHead().equals(stmt)) {
                return true;
            }
        }
        return false;
    }

    static class Edge {
        Stmt src;
        Stmt dst;

        Edge(Stmt src, Stmt dst) {
            this.src = src;
            this.dst = dst;
        }

        @Override
        public int hashCode() {
            int prime = 31;
            int result = 1;
            result = result * prime + src.hashCode();
            result = result * prime + dst.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Edge) {
                Edge other = (Edge) obj;
                return src.equals(other.src) && dst.equals(other.dst);
            }
            return false;
        }
    }
}
