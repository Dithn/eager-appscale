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

package edu.ucsb.cs.eager.sa.bp.tlat;

import edu.ucsb.cs.eager.sa.BranchSelector;
import soot.Unit;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TwoLevelAdaptiveBranchSelector implements BranchSelector {

    private static final int HISTORY_REGISTER_LENGTH = 16;
    private static final double PREDICTOR_ACCURACY_RATE = 0.88;

    private static final Random rand = new Random();

    private Map<Unit,Register> hrt = new HashMap<Unit,Register>();
    private Map<Integer,BranchPattern> pt = new HashMap<Integer, BranchPattern>();

    private BranchPatternFactory patternFactory;

    public TwoLevelAdaptiveBranchSelector(BranchPatternFactory patternFactory) {
        this.patternFactory = patternFactory;
    }

    @Override
    public boolean select(Unit currentInstruction) {
        Register register = getHistoryRegister(currentInstruction);
        BranchPattern pattern = getBranchExecutionPattern(register);
        boolean choice = pattern.select();
        boolean taken;
        if (rand.nextDouble() <= PREDICTOR_ACCURACY_RATE) {
            taken = choice;
        } else {
            taken = !choice;
        }
        register.pushAndShiftLeft(taken);
        pattern.update(taken);
        return taken;
    }

    private Register getHistoryRegister(Unit instruction) {
        Register register = hrt.get(instruction);
        if (register == null) {
            register = new Register(HISTORY_REGISTER_LENGTH);
            hrt.put(instruction, register);
        }
        return register;
    }

    private BranchPattern getBranchExecutionPattern(Register register) {
        int value = register.toInt();
        BranchPattern pattern = pt.get(value);
        if (pattern == null) {
            pattern = patternFactory.create();
            pt.put(value, pattern);
        }
        return pattern;
    }
}
