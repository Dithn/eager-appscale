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

package edu.ucsb.cs.eager.sa.branches.tlat;

public class Register {

    private boolean[] register;

    public Register(int n) {
        if (n > 31) {
            throw new IllegalArgumentException("Bit length must be < 32");
        }
        register = new boolean[n];
    }

    public void pushAndShiftLeft(boolean bit) {
        System.arraycopy(register, 1, register, 0, register.length - 1);
        register[register.length - 1] = bit;
    }

    public int toInt() {
        int n = 0;
        for (boolean r : register) {
            n = (n << 1) + (r ? 1 : 0);
        }
        return n;
    }
}
