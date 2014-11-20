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

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

public class TraceLogParserTest extends TestCase {

    public void testParser() throws Exception {
        InputStream in = TraceLogParserTest.class.getResourceAsStream("/cerebro_sample.txt");
        TraceLogParser parser = new TraceLogParser();
        parser.parse(new BufferedReader(new InputStreamReader(in)));
        Collection<MethodInfo> methods = parser.getMethods();

        String[] expected = new String[] {
            "sfeir.poc.appengine.cs.GoogleCloudStorageServlet#<init>()",
            "sfeir.poc.appengine.cs.GoogleCloudStorageServlet#doGet()",
        };
        int[] expectedPathLengths = new int[] {
            1,
            4,
        };
        assertEquals(expected.length, methods.size());

        int i = 0;
        for (MethodInfo m : methods) {
            assertEquals(expected[i], m.getName());
            assertEquals(1, m.getPaths().size());
            assertEquals(expectedPathLengths[i], m.getPaths().get(0).size());
            i++;
        }
    }
}
