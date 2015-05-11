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

package edu.ucsb.cs.eager.sa.kitty.qbets;

import edu.ucsb.cs.eager.sa.kitty.*;

import java.io.PrintStream;
import java.util.Map;

public class SimpleQBETSPredictionOutput extends AbstractPredictionOutput<Prediction> {

    @Override
    public void write(PrintStream out) {
        int maxLength = 0;
        for (MethodInfo m : results.keySet()) {
            if (m.getName().length() > maxLength) {
                maxLength = m.getName().length();
            }
        }

        for (Map.Entry<MethodInfo,Prediction> entry : results.entrySet()) {
            MethodInfo m = entry.getKey();
            String line = String.format("%-" + maxLength + "s%5d%15s\n", m.getName(),
                    m.getPaths().size(), entry.getValue().toString());
            println(out, line);
        }
    }
}
