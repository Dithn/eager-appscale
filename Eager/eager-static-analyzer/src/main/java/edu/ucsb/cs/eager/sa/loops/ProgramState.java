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

import soot.Value;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ProgramState {

    private Map<Value,IntegerInterval> variables = new HashMap<>();

    public void updateState(Value variable, int upperBound, int lowerBound) {
        updateState(variable, new IntegerInterval(upperBound, lowerBound));
    }

    public void updateState(Value variable, IntegerInterval interval) {
        if (interval == null) {
            variables.remove(variable);
        } else {
            variables.put(variable, interval);
        }
    }

    public void copy(ProgramState state) {
        state.variables.clear();
        for (Map.Entry<Value,IntegerInterval> entry : variables.entrySet()) {
            state.variables.put(entry.getKey(), entry.getValue());
        }
    }

    public IntegerInterval get(Value variable) {
        return variables.get(variable);
    }

    public Collection<Value> getVariables() {
        return variables.keySet();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ProgramState) {
            ProgramState other = (ProgramState) obj;
            if (variables.size() == other.variables.size()) {
                for (Map.Entry<Value,IntegerInterval> entry : variables.entrySet()) {
                    IntegerInterval otherValue = other.get(entry.getKey());
                    if (otherValue == null || !otherValue.equals(entry.getValue())) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Value,IntegerInterval> entry : variables.entrySet()) {
            builder.append(entry.getKey().toString()).append(" --> ").
                    append(entry.getValue().toString());
            builder.append("  ");
        }
        return builder.toString();
    }
}
