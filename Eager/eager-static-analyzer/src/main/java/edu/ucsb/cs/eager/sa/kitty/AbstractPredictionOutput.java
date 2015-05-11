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

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractPredictionOutput<T> implements PredictionOutput {

    protected Map<MethodInfo,T> results = new LinkedHashMap<>();

    public void add(MethodInfo m, T r) {
        results.put(m, r);
    }

    public T get(MethodInfo m) {
        return results.get(m);
    }

    protected void println(PrintStream out, String msg) {
        out.println(msg);
    }

    protected void printTitle(PrintStream out, String text, char underline) {
        println(out, "\n" + text);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            builder.append(underline);
        }
        println(out, builder.toString());
    }

}
