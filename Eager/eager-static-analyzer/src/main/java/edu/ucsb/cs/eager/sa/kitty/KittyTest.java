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

import edu.ucsb.cs.eager.sa.kitty.qbets.QBETSTracingPredictor;

import java.util.ArrayList;
import java.util.List;

public class KittyTest {

    public static void main(String[] args) throws Exception {
        List<MethodInfo> methods = new ArrayList<>();
        int[] counts = new int[]{5,10,15,20,25,30,35};
        for (int c : counts) {
            methods.add(getMethod(c));
        }

        PredictionConfig config = new PredictionConfig();
        config.setBenchmarkDataSvc("http://localhost:8081");
        QBETSTracingPredictor.predict(config, methods);
    }

    private static MethodInfo getMethod(int apiCalls) {
        MethodInfo m = new MethodInfo("testMethod_" + apiCalls);
        Path p = new Path();
        for (int i = 0; i < apiCalls; i++) {
            p.add(new APICall("com.google.appengine.api.datastore.DatastoreService#get()"));
        }
        m.addPath(p);
        return m;
    }
}
